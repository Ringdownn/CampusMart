package repository

import (
	"context"
	"errors"
	"time"

	"campusmart/payment-service/internal/model"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

type AlipayBindRepository struct {
	db *gorm.DB
}

func NewAlipayBindRepository(db *gorm.DB) *AlipayBindRepository {
	return &AlipayBindRepository{db: db}
}

func (r *AlipayBindRepository) Upsert(ctx context.Context, bind *model.AlipayAccountBind) error {
	now := time.Now()
	if bind.BindTime.IsZero() {
		bind.BindTime = now
	}
	return r.db.WithContext(ctx).
		Clauses(clause.OnConflict{
			Columns: []clause.Column{{Name: "user_id"}},
			DoUpdates: clause.Assignments(map[string]interface{}{
				"alipay_user_id":  bind.AlipayUserID,
				"alipay_login_id": bind.AlipayLoginID,
				"nickname":        bind.Nickname,
				"access_token":    bind.AccessToken,
				"refresh_token":   bind.RefreshToken,
				"bind_time":       bind.BindTime,
				"is_deleted":      0,
				"update_time":     now,
			}),
		}).
		Create(bind).Error
}

func (r *AlipayBindRepository) FindByUserID(ctx context.Context, userID int64) (*model.AlipayAccountBind, error) {
	var bind model.AlipayAccountBind
	err := r.db.WithContext(ctx).
		Where("user_id = ? AND is_deleted = 0", userID).
		First(&bind).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &bind, nil
}

func (r *AlipayBindRepository) SoftDeleteByUserID(ctx context.Context, userID int64) error {
	return r.db.WithContext(ctx).
		Model(&model.AlipayAccountBind{}).
		Where("user_id = ? AND is_deleted = 0", userID).
		Updates(map[string]interface{}{
			"is_deleted":  1,
			"update_time": time.Now(),
		}).Error
}
