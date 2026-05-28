package repository

import (
	"context"
	"errors"
	"time"

	"campusmart/wallet-service/internal/model"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

type WalletRepository struct {
	db *gorm.DB
}

func NewWalletRepository(db *gorm.DB) *WalletRepository {
	return &WalletRepository{db: db}
}

func (r *WalletRepository) GetOrCreateWallet(ctx context.Context, userID int64) (*model.UserWallet, error) {
	var wallet model.UserWallet
	err := r.db.WithContext(ctx).Where("user_id = ? AND is_deleted = 0", userID).First(&wallet).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		wallet = model.UserWallet{
			UserID:          userID,
			AvailableAmount: "0.00",
			FrozenAmount:    "0.00",
		}
		if err := r.db.WithContext(ctx).Create(&wallet).Error; err != nil {
			return nil, err
		}
		return &wallet, nil
	}
	if err != nil {
		return nil, err
	}
	return &wallet, nil
}

func (r *WalletRepository) ListFlows(ctx context.Context, userID int64, limit int) ([]model.WalletFlow, error) {
	if limit <= 0 || limit > 100 {
		limit = 50
	}
	var flows []model.WalletFlow
	err := r.db.WithContext(ctx).
		Where("user_id = ? AND is_deleted = 0", userID).
		Order("id DESC").
		Limit(limit).
		Find(&flows).Error
	return flows, err
}

func (r *WalletRepository) AddEscrowFlow(ctx context.Context, userID, orderID int64, orderNo, amount string) error {
	return r.createOrderFlowIfAbsent(ctx, userID, orderID, orderNo, model.FlowTypeEscrowIn, amount, "平台托管入账")
}

func (r *WalletRepository) AddSellerIncome(ctx context.Context, sellerID, orderID int64, orderNo, amount string) error {
	return r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		wallet, err := r.getOrCreateWalletForUpdate(ctx, tx, sellerID)
		if err != nil {
			return err
		}
		current, err := amountToCents(wallet.AvailableAmount)
		if err != nil {
			return err
		}
		income, err := amountToCents(amount)
		if err != nil {
			return err
		}
		balanceAfter := current + income
		if err := tx.Model(&model.UserWallet{}).
			Where("id = ?", wallet.ID).
			Updates(map[string]interface{}{
				"available_amount": centsToAmount(balanceAfter),
				"update_time":      time.Now(),
			}).Error; err != nil {
			return err
		}
		orderIDPtr := orderID
		orderNoPtr := orderNo
		return tx.Clauses(clause.OnConflict{DoNothing: true}).Create(&model.WalletFlow{
			UserID:       sellerID,
			OrderID:      &orderIDPtr,
			OrderNo:      &orderNoPtr,
			FlowNo:       newFlowNo("WF"),
			FlowType:     model.FlowTypeSellerIncome,
			Amount:       amount,
			BalanceAfter: centsToAmount(balanceAfter),
			Remark:       "卖家确认收货到账",
		}).Error
	})
}

func (r *WalletRepository) Withdraw(ctx context.Context, userID int64, amount string) (*model.UserWallet, *model.WalletFlow, error) {
	var updatedWallet *model.UserWallet
	var createdFlow *model.WalletFlow
	err := r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		wallet, err := r.getOrCreateWalletForUpdate(ctx, tx, userID)
		if err != nil {
			return err
		}
		current, err := amountToCents(wallet.AvailableAmount)
		if err != nil {
			return err
		}
		withdrawAmount, err := amountToCents(amount)
		if err != nil || withdrawAmount <= 0 {
			return ErrInvalidAmount
		}
		if current < withdrawAmount {
			return ErrInsufficientBalance
		}

		balanceAfter := current - withdrawAmount
		if err := tx.Model(&model.UserWallet{}).
			Where("id = ?", wallet.ID).
			Updates(map[string]interface{}{
				"available_amount": centsToAmount(balanceAfter),
				"update_time":      time.Now(),
			}).Error; err != nil {
			return err
		}

		flow := &model.WalletFlow{
			UserID:       userID,
			FlowNo:       newFlowNo("WD"),
			FlowType:     model.FlowTypeWithdrawOut,
			Amount:       centsToAmount(withdrawAmount),
			BalanceAfter: centsToAmount(balanceAfter),
			Remark:       "模拟提现扣款",
		}
		if err := tx.Create(flow).Error; err != nil {
			return err
		}
		wallet.AvailableAmount = centsToAmount(balanceAfter)
		updatedWallet = wallet
		createdFlow = flow
		return nil
	})
	return updatedWallet, createdFlow, err
}

func (r *WalletRepository) createOrderFlowIfAbsent(ctx context.Context, userID, orderID int64, orderNo, flowType, amount, remark string) error {
	orderIDPtr := orderID
	orderNoPtr := orderNo
	return r.db.WithContext(ctx).Clauses(clause.OnConflict{DoNothing: true}).Create(&model.WalletFlow{
		UserID:       userID,
		OrderID:      &orderIDPtr,
		OrderNo:      &orderNoPtr,
		FlowNo:       newFlowNo("WF"),
		FlowType:     flowType,
		Amount:       amount,
		BalanceAfter: "0.00",
		Remark:       remark,
	}).Error
}

func (r *WalletRepository) getOrCreateWalletForUpdate(ctx context.Context, tx *gorm.DB, userID int64) (*model.UserWallet, error) {
	var wallet model.UserWallet
	err := tx.Clauses(clause.Locking{Strength: "UPDATE"}).
		Where("user_id = ? AND is_deleted = 0", userID).
		First(&wallet).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		wallet = model.UserWallet{
			UserID:          userID,
			AvailableAmount: "0.00",
			FrozenAmount:    "0.00",
		}
		if err := tx.WithContext(ctx).Create(&wallet).Error; err != nil {
			return nil, err
		}
		return &wallet, nil
	}
	if err != nil {
		return nil, err
	}
	return &wallet, nil
}

var (
	ErrInvalidAmount       = errors.New("提现金额不正确")
	ErrInsufficientBalance = errors.New("钱包余额不足")
)
