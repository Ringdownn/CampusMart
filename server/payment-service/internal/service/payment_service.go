package service

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"net/url"
	"regexp"
	"strconv"
	"strings"
	"time"

	"campusmart/payment-service/internal/model"
	"campusmart/payment-service/internal/repository"
)

var (
	ErrInvalidPaymentParam = errors.New("支付参数不正确")
	ErrPaymentNotFound     = errors.New("支付单不存在")
	ErrPaymentNotPayable   = errors.New("支付单不可支付")
	ErrPaymentForbidden    = errors.New("无权操作该支付单")
	ErrAlipayNotBound      = errors.New("请先绑定支付宝沙箱账户")
)

var amountPattern = regexp.MustCompile(`^[0-9]+(\.[0-9]{1,2})?$`)

type PaymentConfig struct {
	AlipayAppID           string
	AlipayNotifyURL       string
	AlipayMockSignSecret  string
	AlipayVerifySignature bool
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
	OutTradeNo    string `json:"out_trade_no"`
	TradeNo       string `json:"trade_no"`
	TotalAmount   string `json:"total_amount"`
	TradeStatus   string `json:"trade_status"`
	AppID         string `json:"app_id"`
	Sign          string `json:"sign"`
	RawBody       string `json:"-"`
	RawSignSource string `json:"-"`
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
}

func NewPaymentService(paymentRepo *repository.PaymentRepository, bindRepo *repository.AlipayBindRepository, cfg PaymentConfig) *PaymentService {
	return &PaymentService{
		paymentRepo: paymentRepo,
		bindRepo:    bindRepo,
		cfg:         cfg,
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

	values := url.Values{}
	values.Set("app_id", s.cfg.AlipayAppID)
	values.Set("method", "alipay.trade.app.pay")
	values.Set("charset", "utf-8")
	values.Set("sign_type", "RSA2")
	values.Set("timestamp", time.Now().Format("2006-01-02 15:04:05"))
	values.Set("version", "1.0")
	values.Set("notify_url", s.cfg.AlipayNotifyURL)
	values.Set("out_trade_no", payment.PayNo)
	values.Set("subject", "CampusMart订单-"+payment.OrderNo)
	values.Set("total_amount", payment.Amount)
	values.Set("product_code", "QUICK_MSECURITY_PAY")
	values.Set("sandbox_buyer", bind.AlipayLoginID)
	values.Set("mock", "true")

	sign := signMock(values.Encode(), s.cfg.AlipayMockSignSecret)
	values.Set("sign", sign)

	return &AlipayPayResponse{
		OrderID:     payment.OrderID,
		OrderNo:     payment.OrderNo,
		PayNo:       payment.PayNo,
		OrderString: values.Encode(),
	}, nil
}

func (s *PaymentService) HandleAlipayNotify(ctx context.Context, req AlipayNotifyRequest) (bool, error) {
	req.OutTradeNo = strings.TrimSpace(req.OutTradeNo)
	req.TradeNo = strings.TrimSpace(req.TradeNo)
	req.TotalAmount = normalizeAmount(req.TotalAmount)
	if req.OutTradeNo == "" || req.TradeNo == "" || !validAmount(req.TotalAmount) {
		return false, ErrInvalidPaymentParam
	}
	if req.TradeStatus == "" {
		req.TradeStatus = "TRADE_SUCCESS"
	}
	if req.TradeStatus != "TRADE_SUCCESS" && req.TradeStatus != "TRADE_FINISHED" {
		return false, nil
	}
	if s.cfg.AlipayVerifySignature && !verifyMockSignature(req.RawSignSource, req.Sign, s.cfg.AlipayMockSignSecret) {
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

func signMock(source, secret string) string {
	mac := hmac.New(sha256.New, []byte(secret))
	_, _ = mac.Write([]byte(source))
	return base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
}

func verifyMockSignature(source, sign, secret string) bool {
	if source == "" || sign == "" {
		return false
	}
	return hmac.Equal([]byte(signMock(source, secret)), []byte(sign))
}
