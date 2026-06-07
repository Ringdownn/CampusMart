package service

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"regexp"
	"strconv"
	"strings"
	"time"

	"campusmart/payment-service/internal/model"
	alipayclient "campusmart/payment-service/internal/pkg/alipay"
	"campusmart/payment-service/internal/repository"
)

var (
	ErrInvalidPaymentParam = errors.New("支付参数不正确")
	ErrPaymentNotFound     = errors.New("支付单不存在")
	ErrPaymentNotPayable   = errors.New("支付单不可支付")
	ErrPaymentForbidden    = errors.New("无权操作该支付单")
	ErrAlipayNotBound      = errors.New("请先绑定支付宝沙箱账户")
	ErrAlipayConfig        = errors.New("支付宝沙箱配置不完整")
)

var amountPattern = regexp.MustCompile(`^[0-9]+(\.[0-9]{1,2})?$`)

type PaymentConfig struct {
	AlipayAppID         string
	AlipayNotifyURL     string
	AlipayAppPrivateKey string
	AlipayPublicKey     string
	AlipayIsProduction  bool
}

type CreatePaymentRequest struct {
	OrderID int64  `json:"orderId"`
	OrderNo string `json:"orderNo"`
	BuyerID int64  `json:"buyerId"`
	Amount  string `json:"amount"`
}

type CreatePaymentResponse struct {
	OrderID int64  `json:"orderId"`
	OrderNo string `json:"orderNo"`
	PayNo   string `json:"payNo"`
	Status  string `json:"status"`
	Amount  string `json:"amount"`
}

type AlipayPayResponse struct {
	OrderID     int64  `json:"orderId"`
	OrderNo     string `json:"orderNo"`
	PayNo       string `json:"payNo"`
	OrderString string `json:"orderString"`
}

type AlipayNotifyRequest struct {
	OutTradeNo  string            `json:"out_trade_no"`
	TradeNo     string            `json:"trade_no"`
	TotalAmount string            `json:"total_amount"`
	TradeStatus string            `json:"trade_status"`
	AppID       string            `json:"app_id"`
	Sign        string            `json:"sign"`
	Params      map[string]string `json:"-"`
	RawBody     string            `json:"-"`
}

type ClosePaymentResponse struct {
	OrderID int64  `json:"orderId"`
	Closed  bool   `json:"closed"`
	Status  string `json:"status"`
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

type PaymentService struct {
	paymentRepo *repository.PaymentRepository
	bindRepo    *repository.AlipayBindRepository
	cfg         PaymentConfig
	alipay      *alipayclient.Client
	alipayErr   error
}

func NewPaymentService(paymentRepo *repository.PaymentRepository, bindRepo *repository.AlipayBindRepository, cfg PaymentConfig) *PaymentService {
	alipay, err := alipayclient.NewClient(alipayclient.Config{
		AppID:         cfg.AlipayAppID,
		AppPrivateKey: cfg.AlipayAppPrivateKey,
		AlipayKey:     cfg.AlipayPublicKey,
		NotifyURL:     cfg.AlipayNotifyURL,
		IsProduction:  cfg.AlipayIsProduction,
	})
	return &PaymentService{
		paymentRepo: paymentRepo,
		bindRepo:    bindRepo,
		cfg:         cfg,
		alipay:      alipay,
		alipayErr:   err,
	}
}

func (s *PaymentService) CreatePayment(ctx context.Context, req CreatePaymentRequest) (*CreatePaymentResponse, error) {
	req.Amount = normalizeAmount(req.Amount)
	if req.OrderID <= 0 || strings.TrimSpace(req.OrderNo) == "" || req.BuyerID <= 0 || !validAmount(req.Amount) {
		return nil, ErrInvalidPaymentParam
	}

	existing, err := s.paymentRepo.FindByOrderID(ctx, req.OrderID)
	if err != nil {
		return nil, err
	}
	if existing != nil {
		return toCreatePaymentResponse(existing), nil
	}

	payment := &model.Payment{
		OrderID: req.OrderID,
		OrderNo: strings.TrimSpace(req.OrderNo),
		PayNo:   newPaymentNo(),
		BuyerID: req.BuyerID,
		Amount:  req.Amount,
		Channel: model.PaymentChannelAlipaySandbox,
		Status:  model.PaymentStatusWaitPay,
	}
	if err := s.paymentRepo.Create(ctx, payment); err != nil {
		return nil, err
	}
	return toCreatePaymentResponse(payment), nil
}

func (s *PaymentService) BuildAlipayOrderString(ctx context.Context, orderID, currentUserID int64) (*AlipayPayResponse, error) {
	payment, err := s.paymentRepo.FindByOrderID(ctx, orderID)
	if err != nil {
		return nil, err
	}
	if payment == nil {
		return nil, ErrPaymentNotFound
	}
	if payment.BuyerID != currentUserID {
		return nil, ErrPaymentForbidden
	}
	if payment.Status != model.PaymentStatusWaitPay {
		return nil, ErrPaymentNotPayable
	}

	bind, err := s.bindRepo.FindByUserID(ctx, currentUserID)
	if err != nil {
		return nil, err
	}
	if bind == nil {
		return nil, ErrAlipayNotBound
	}

	if s.alipayErr != nil || s.alipay == nil {
		return nil, ErrAlipayConfig
	}
	orderString, err := s.alipay.BuildAppPayOrder(alipayclient.AppPayOrder{
		OutTradeNo:  payment.PayNo,
		Subject:     "CampusMart订单-" + payment.OrderNo,
		TotalAmount: payment.Amount,
		Body:        "CampusMart校园交易订单",
	})
	if err != nil {
		return nil, err
	}

	return &AlipayPayResponse{
		OrderID:     payment.OrderID,
		OrderNo:     payment.OrderNo,
		PayNo:       payment.PayNo,
		OrderString: orderString,
	}, nil
}

func (s *PaymentService) HandleAlipayNotify(ctx context.Context, req AlipayNotifyRequest) (bool, error) {
	req.OutTradeNo = strings.TrimSpace(req.OutTradeNo)
	req.TradeNo = strings.TrimSpace(req.TradeNo)
	req.TotalAmount = normalizeAmount(req.TotalAmount)
	if req.OutTradeNo == "" || req.TradeNo == "" || !validAmount(req.TotalAmount) {
		return false, ErrInvalidPaymentParam
	}
	if req.TradeStatus != "TRADE_SUCCESS" && req.TradeStatus != "TRADE_FINISHED" {
		return false, nil
	}
	if req.AppID != s.cfg.AlipayAppID {
		return false, ErrInvalidPaymentParam
	}
	if s.alipayErr != nil || s.alipay == nil {
		return false, ErrAlipayConfig
	}
	if !s.alipay.VerifyNotify(req.Params) {
		diag := s.alipay.VerifyNotifyDiagnostics(req.Params)
		log.Printf(
			"alipay notify verify failed out_trade_no=%s trade_no=%s exclude_sign_type=%t include_sign_type=%t sign_type=%s sign_len=%d params=%d",
			req.OutTradeNo,
			req.TradeNo,
			diag.ExcludingSignType,
			diag.IncludingSignType,
			req.Params["sign_type"],
			len(req.Params["sign"]),
			len(req.Params),
		)
		return false, ErrInvalidPaymentParam
	}

	currentPayment, err := s.paymentRepo.FindByPayNo(ctx, req.OutTradeNo)
	if err != nil {
		return false, err
	}
	if currentPayment == nil {
		return false, ErrPaymentNotFound
	}
	if currentPayment.Amount != req.TotalAmount {
		return false, ErrInvalidPaymentParam
	}

	payment, updated, err := s.paymentRepo.MarkSuccess(ctx, req.OutTradeNo, req.TradeNo, req.RawBody)
	if err != nil {
		return false, err
	}
	if payment == nil {
		return false, ErrPaymentNotFound
	}
	if updated {
		if err := s.createPaymentPaidOutbox(ctx, payment); err != nil {
			return false, err
		}
	}
	return true, nil
}

func (s *PaymentService) ClosePayment(ctx context.Context, orderID int64) (*ClosePaymentResponse, error) {
	if orderID <= 0 {
		return nil, ErrInvalidPaymentParam
	}
	closed, err := s.paymentRepo.CloseByOrderID(ctx, orderID)
	if err != nil {
		return nil, err
	}
	status := model.PaymentStatusClosed
	if !closed {
		payment, err := s.paymentRepo.FindByOrderID(ctx, orderID)
		if err != nil {
			return nil, err
		}
		if payment == nil {
			return nil, ErrPaymentNotFound
		}
		status = payment.Status
	}
	return &ClosePaymentResponse{OrderID: orderID, Closed: closed, Status: status}, nil
}

func (s *PaymentService) createPaymentPaidOutbox(ctx context.Context, payment *model.Payment) error {
	eventID := fmt.Sprintf("payment-paid-%s", payment.PayNo)
	payload, err := json.Marshal(PaymentPaidEvent{
		EventID:       eventID,
		OrderID:       payment.OrderID,
		OrderNo:       payment.OrderNo,
		PayNo:         payment.PayNo,
		BuyerID:       payment.BuyerID,
		Amount:        payment.Amount,
		AlipayTradeNo: payment.AlipayTradeNo,
	})
	if err != nil {
		return err
	}
	return s.paymentRepo.CreateOutbox(ctx, &model.PaymentOutbox{
		EventID:    eventID,
		EventType:  "payment.paid",
		RoutingKey: "payment.paid",
		Payload:    string(payload),
		Status:     "NEW",
	})
}

func toCreatePaymentResponse(payment *model.Payment) *CreatePaymentResponse {
	return &CreatePaymentResponse{
		OrderID: payment.OrderID,
		OrderNo: payment.OrderNo,
		PayNo:   payment.PayNo,
		Status:  payment.Status,
		Amount:  payment.Amount,
	}
}

func newPaymentNo() string {
	return "PAY" + time.Now().Format("20060102150405") + strconv.FormatInt(time.Now().UnixNano()%1_000_000, 10)
}

func normalizeAmount(amount string) string {
	amount = strings.TrimSpace(amount)
	if amount == "" {
		return amount
	}
	if !strings.Contains(amount, ".") {
		return amount + ".00"
	}
	parts := strings.SplitN(amount, ".", 2)
	if len(parts[1]) == 1 {
		return amount + "0"
	}
	return amount
}

func validAmount(amount string) bool {
	return amountPattern.MatchString(amount) && amount != "0.00"
}
