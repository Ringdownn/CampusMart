package service

import (
	"context"
	"encoding/json"
	"errors"
	"strconv"
	"time"

	"campusmart/trade-service/internal/client"
	"campusmart/trade-service/internal/model"
	"campusmart/trade-service/internal/repository"
)

var (
	ErrInvalidParam       = errors.New("参数不正确")
	ErrOrderNotFound      = errors.New("订单不存在")
	ErrOrderForbidden     = errors.New("无权操作该订单")
	ErrOrderNotPayable    = errors.New("订单不可取消")
	ErrOrderNotSettleable = errors.New("订单不可确认收货")
)

type CreateOrderRequest struct {
	GoodsID int64 `json:"goodsId"`
}

type OrderService struct {
	repo              *repository.OrderRepository
	paymentClient     *client.PaymentClient
	payTimeoutMinutes int
	timeoutPublisher  TimeoutPublisher
}

type TimeoutPublisher interface {
	PublishPaymentTimeout(ctx context.Context, orderID int64, orderNo string, delay time.Duration) error
}

func NewOrderService(repo *repository.OrderRepository, paymentClient *client.PaymentClient, payTimeoutMinutes int, timeoutPublisher TimeoutPublisher) *OrderService {
	return &OrderService{
		repo:              repo,
		paymentClient:     paymentClient,
		payTimeoutMinutes: payTimeoutMinutes,
		timeoutPublisher:  timeoutPublisher,
	}
}

func (s *OrderService) CreateOrder(ctx context.Context, buyerID int64, req CreateOrderRequest) (*model.Order, error) {
	if buyerID <= 0 || req.GoodsID <= 0 {
		return nil, ErrInvalidParam
	}

	orderNo := newOrderNo()
	payExpireAt := time.Now().Add(time.Duration(s.payTimeoutMinutes) * time.Minute)
	order, err := s.repo.CreateWithGoodsLock(ctx, req.GoodsID, buyerID, payExpireAt, orderNo, "0.00")
	if err != nil {
		return nil, err
	}

	if _, err := s.paymentClient.CreatePayment(ctx, client.CreatePaymentRequest{
		OrderID: order.ID,
		OrderNo: order.OrderNo,
		BuyerID: order.BuyerID,
		Amount:  order.Amount,
	}); err != nil {
		_, _, _ = s.repo.CancelCreated(ctx, order.ID, "CREATE_PAYMENT_FAILED")
		return nil, err
	}

	if s.timeoutPublisher != nil {
		_ = s.timeoutPublisher.PublishPaymentTimeout(ctx, order.ID, order.OrderNo, time.Duration(s.payTimeoutMinutes)*time.Minute)
	}
	return order, nil
}

func (s *OrderService) GetOrder(ctx context.Context, currentUserID, orderID int64) (*model.Order, error) {
	order, err := s.repo.FindByID(ctx, orderID)
	if err != nil {
		return nil, err
	}
	if order == nil {
		return nil, ErrOrderNotFound
	}
	if order.BuyerID != currentUserID && order.SellerID != currentUserID {
		return nil, ErrOrderForbidden
	}
	return order, nil
}

func (s *OrderService) ListBuyerOrders(ctx context.Context, buyerID int64) ([]model.Order, error) {
	return s.repo.ListByBuyer(ctx, buyerID)
}

func (s *OrderService) ListSellerOrders(ctx context.Context, sellerID int64) ([]model.Order, error) {
	return s.repo.ListBySeller(ctx, sellerID)
}

func (s *OrderService) CancelOrder(ctx context.Context, currentUserID, orderID int64, reason string) (*model.Order, error) {
	order, err := s.repo.FindByID(ctx, orderID)
	if err != nil {
		return nil, err
	}
	if order == nil {
		return nil, ErrOrderNotFound
	}
	if order.BuyerID != currentUserID {
		return nil, ErrOrderForbidden
	}
	if order.Status != model.OrderStatusCreated {
		return nil, ErrOrderNotPayable
	}

	updatedOrder, updated, err := s.repo.CancelCreated(ctx, orderID, reason)
	if err != nil {
		return nil, err
	}
	if updated {
		_ = s.paymentClient.ClosePayment(ctx, orderID)
	}
	return updatedOrder, nil
}

func (s *OrderService) TimeoutCancel(ctx context.Context, orderID int64, orderNo string) error {
	order, updated, err := s.repo.CancelCreated(ctx, orderID, "PAY_TIMEOUT")
	if err != nil {
		return err
	}
	if updated && order != nil {
		_ = s.paymentClient.ClosePayment(ctx, orderID)
	}
	return nil
}

func (s *OrderService) MarkPaid(ctx context.Context, event PaymentPaidEvent, raw string) error {
	_, _, err := s.repo.MarkPaid(ctx, event.OrderID, raw)
	return err
}

func (s *OrderService) ConfirmReceipt(ctx context.Context, currentUserID, orderID int64) (*model.Order, error) {
	order, err := s.repo.FindByID(ctx, orderID)
	if err != nil {
		return nil, err
	}
	if order == nil {
		return nil, ErrOrderNotFound
	}
	if order.BuyerID != currentUserID {
		return nil, ErrOrderForbidden
	}
	if order.Status != model.OrderStatusPaid {
		return nil, ErrOrderNotSettleable
	}

	payload, err := json.Marshal(map[string]interface{}{
		"eventId":   "trade-order-settled-" + order.OrderNo,
		"orderId":   order.ID,
		"orderNo":   order.OrderNo,
		"buyerId":   order.BuyerID,
		"sellerId":  order.SellerID,
		"amount":    order.Amount,
		"settledAt": time.Now().Format("2006-01-02 15:04:05"),
	})
	if err != nil {
		return nil, err
	}

	settled, updated, err := s.repo.SettlePaid(ctx, orderID, string(payload))
	if err != nil {
		return nil, err
	}
	if !updated {
		return nil, ErrOrderNotSettleable
	}
	return settled, nil
}

type PaymentPaidEvent struct {
	EventID       string `json:"eventId"`
	OrderID       int64  `json:"orderId"`
	OrderNo       string `json:"orderNo"`
	PayNo         string `json:"payNo"`
	BuyerID       int64  `json:"buyerId"`
	Amount        string `json:"amount"`
	AlipayTradeNo string `json:"alipayTradeNo"`
}

func newOrderNo() string {
	return "CM" + time.Now().Format("20060102150405") + strconv.FormatInt(time.Now().UnixNano()%1_000_000, 10)
}
