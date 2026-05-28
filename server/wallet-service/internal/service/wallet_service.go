package service

import (
	"context"
	"errors"
	"strings"

	"campusmart/wallet-service/internal/model"
	"campusmart/wallet-service/internal/repository"
)

var ErrInvalidParam = errors.New("参数不正确")

type WalletService struct {
	repo *repository.WalletRepository
}

func NewWalletService(repo *repository.WalletRepository) *WalletService {
	return &WalletService{repo: repo}
}

type WithdrawRequest struct {
	Amount string `json:"amount"`
}

type WithdrawResponse struct {
	UserID          int64  `json:"userId"`
	WithdrawAmount  string `json:"withdrawAmount"`
	AvailableAmount string `json:"availableAmount"`
	FlowType        string `json:"flowType"`
	Message         string `json:"message"`
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

type OrderSettledEvent struct {
	EventID   string `json:"eventId"`
	OrderID   int64  `json:"orderId"`
	OrderNo   string `json:"orderNo"`
	BuyerID   int64  `json:"buyerId"`
	SellerID  int64  `json:"sellerId"`
	Amount    string `json:"amount"`
	SettledAt string `json:"settledAt"`
}

func (s *WalletService) GetWallet(ctx context.Context, userID int64) (*model.UserWallet, error) {
	if userID <= 0 {
		return nil, ErrInvalidParam
	}
	return s.repo.GetOrCreateWallet(ctx, userID)
}

func (s *WalletService) ListFlows(ctx context.Context, userID int64, limit int) ([]model.WalletFlow, error) {
	if userID <= 0 {
		return nil, ErrInvalidParam
	}
	return s.repo.ListFlows(ctx, userID, limit)
}

func (s *WalletService) Withdraw(ctx context.Context, userID int64, req WithdrawRequest) (*WithdrawResponse, error) {
	if userID <= 0 || strings.TrimSpace(req.Amount) == "" {
		return nil, ErrInvalidParam
	}
	wallet, flow, err := s.repo.Withdraw(ctx, userID, req.Amount)
	if err != nil {
		return nil, err
	}
	return &WithdrawResponse{
		UserID:          userID,
		WithdrawAmount:  flow.Amount,
		AvailableAmount: wallet.AvailableAmount,
		FlowType:        model.FlowTypeWithdrawOut,
		Message:         "模拟提现成功",
	}, nil
}

func (s *WalletService) HandlePaymentPaid(ctx context.Context, event PaymentPaidEvent) error {
	if event.OrderID <= 0 || event.BuyerID <= 0 || event.Amount == "" {
		return ErrInvalidParam
	}
	return s.repo.AddEscrowFlow(ctx, event.BuyerID, event.OrderID, event.OrderNo, event.Amount)
}

func (s *WalletService) HandleOrderSettled(ctx context.Context, event OrderSettledEvent) error {
	if event.OrderID <= 0 || event.SellerID <= 0 || event.Amount == "" {
		return ErrInvalidParam
	}
	return s.repo.AddSellerIncome(ctx, event.SellerID, event.OrderID, event.OrderNo, event.Amount)
}
