package repository

import (
	"context"
	"errors"
	"time"

	"campusmart/payment-service/internal/model"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

type PaymentRepository struct {
	db *gorm.DB
}

func NewPaymentRepository(db *gorm.DB) *PaymentRepository {
	return &PaymentRepository{db: db}
}

func (r *PaymentRepository) Create(ctx context.Context, payment *model.Payment) error {
	return r.db.WithContext(ctx).
		Clauses(clause.OnConflict{
			Columns:   []clause.Column{{Name: "order_id"}},
			DoNothing: true,
		}).
		Create(payment).Error
}

func (r *PaymentRepository) FindByOrderID(ctx context.Context, orderID int64) (*model.Payment, error) {
	var payment model.Payment
	err := r.db.WithContext(ctx).
		Where("order_id = ? AND is_deleted = 0", orderID).
		First(&payment).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &payment, nil
}

func (r *PaymentRepository) FindByPayNo(ctx context.Context, payNo string) (*model.Payment, error) {
	var payment model.Payment
	err := r.db.WithContext(ctx).
		Where("pay_no = ? AND is_deleted = 0", payNo).
		First(&payment).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &payment, nil
}

func (r *PaymentRepository) MarkSuccess(ctx context.Context, payNo, alipayTradeNo, notifyBody string) (*model.Payment, bool, error) {
	var updatedPayment *model.Payment
	updated := false

	err := r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		var payment model.Payment
		if err := tx.Clauses(clause.Locking{Strength: "UPDATE"}).
			Where("pay_no = ? AND is_deleted = 0", payNo).
			First(&payment).Error; err != nil {
			return err
		}

		if payment.Status == model.PaymentStatusSuccess {
			updatedPayment = &payment
			return nil
		}
		if payment.Status != model.PaymentStatusWaitPay {
			updatedPayment = &payment
			return nil
		}

		now := time.Now()
		result := tx.Model(&model.Payment{}).
			Where("id = ? AND status = ?", payment.ID, model.PaymentStatusWaitPay).
			Updates(map[string]interface{}{
				"status":          model.PaymentStatusSuccess,
				"alipay_trade_no": alipayTradeNo,
				"notify_body":     notifyBody,
				"paid_at":         now,
				"update_time":     now,
			})
		if result.Error != nil {
			return result.Error
		}
		if result.RowsAffected == 1 {
			payment.Status = model.PaymentStatusSuccess
			payment.AlipayTradeNo = alipayTradeNo
			payment.NotifyBody = notifyBody
			payment.PaidAt = &now
			payment.UpdateTime = now
			updated = true
		}
		updatedPayment = &payment
		return nil
	})
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, false, nil
	}
	if err != nil {
		return nil, false, err
	}
	return updatedPayment, updated, nil
}

func (r *PaymentRepository) CloseByOrderID(ctx context.Context, orderID int64) (bool, error) {
	now := time.Now()
	result := r.db.WithContext(ctx).Model(&model.Payment{}).
		Where("order_id = ? AND status = ? AND is_deleted = 0", orderID, model.PaymentStatusWaitPay).
		Updates(map[string]interface{}{
			"status":      model.PaymentStatusClosed,
			"closed_at":   now,
			"update_time": now,
		})
	if result.Error != nil {
		return false, result.Error
	}
	return result.RowsAffected == 1, nil
}

func (r *PaymentRepository) CreateOutbox(ctx context.Context, outbox *model.PaymentOutbox) error {
	return r.db.WithContext(ctx).
		Clauses(clause.OnConflict{
			Columns:   []clause.Column{{Name: "event_id"}},
			DoNothing: true,
		}).
		Create(outbox).Error
}

func (r *PaymentRepository) ListPendingOutbox(ctx context.Context, limit int) ([]model.PaymentOutbox, error) {
	if limit <= 0 {
		limit = 20
	}

	var events []model.PaymentOutbox
	err := r.db.WithContext(ctx).
		Where("status = ? AND (next_retry_at IS NULL OR next_retry_at <= ?)", "NEW", time.Now()).
		Order("id ASC").
		Limit(limit).
		Find(&events).Error
	return events, err
}

func (r *PaymentRepository) MarkOutboxSent(ctx context.Context, id int64) error {
	return r.db.WithContext(ctx).
		Model(&model.PaymentOutbox{}).
		Where("id = ? AND status = ?", id, "NEW").
		Updates(map[string]interface{}{
			"status":      "SENT",
			"update_time": time.Now(),
		}).Error
}

func (r *PaymentRepository) MarkOutboxFailed(ctx context.Context, id int64) error {
	now := time.Now()
	return r.db.WithContext(ctx).
		Model(&model.PaymentOutbox{}).
		Where("id = ? AND status = ?", id, "NEW").
		Updates(map[string]interface{}{
			"retry_count":   gorm.Expr("retry_count + 1"),
			"next_retry_at": now.Add(30 * time.Second),
			"update_time":   now,
		}).Error
}
