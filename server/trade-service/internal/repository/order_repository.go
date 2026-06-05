package repository

import (
	"context"
	"errors"
	"fmt"
	"time"

	"campusmart/trade-service/internal/model"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

type OrderRepository struct {
	db *gorm.DB
}

func NewOrderRepository(db *gorm.DB) *OrderRepository {
	return &OrderRepository{db: db}
}

func (r *OrderRepository) CreateWithGoodsLock(ctx context.Context, goodsID, buyerID int64, payExpireAt time.Time, orderNo string) (*model.Order, error) {
	var created *model.Order
	err := r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		var goods model.Goods
		if err := tx.Clauses(clause.Locking{Strength: "UPDATE"}).
			Where("goodID = ?", goodsID).
			First(&goods).Error; err != nil {
			return err
		}
		if goods.PublishUserID == buyerID {
			return ErrBuyOwnGoods
		}

		var activeCount int64
		if err := tx.Model(&model.Order{}).
			Where("goods_id = ? AND status IN ?", goodsID, []string{model.OrderStatusCreated, model.OrderStatusPaid, model.OrderStatusSettled}).
			Count(&activeCount).Error; err != nil {
			return err
		}
		if activeCount > 0 {
			return ErrGoodsAlreadyOrdered
		}

		order := &model.Order{
			OrderNo:     orderNo,
			GoodsID:     goods.GoodID,
			BuyerID:     buyerID,
			SellerID:    goods.PublishUserID,
			Amount:      fmt.Sprintf("%d.00", goods.Price),
			Status:      model.OrderStatusCreated,
			PayExpireAt: payExpireAt,
		}
		if err := tx.Create(order).Error; err != nil {
			return err
		}
		if err := tx.Create(&model.OrderEvent{
			OrderID:   order.ID,
			OrderNo:   order.OrderNo,
			ToStatus:  model.OrderStatusCreated,
			EventType: "order.created",
		}).Error; err != nil {
			return err
		}
		created = order
		return nil
	})
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, ErrGoodsNotFound
	}
	return created, err
}

func (r *OrderRepository) FindByID(ctx context.Context, orderID int64) (*model.Order, error) {
	var order model.Order
	err := r.db.WithContext(ctx).
		Where("id = ? AND is_deleted = 0", orderID).
		First(&order).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil
	}
	return &order, err
}

func (r *OrderRepository) FindLatestByGoodsAndParties(ctx context.Context, goodsID, buyerID, sellerID int64) (*model.Order, error) {
	var order model.Order
	err := r.db.WithContext(ctx).
		Where("goods_id = ? AND buyer_id = ? AND seller_id = ? AND is_deleted = 0", goodsID, buyerID, sellerID).
		Order("id DESC").
		First(&order).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, nil
	}
	return &order, err
}

func (r *OrderRepository) ListByBuyer(ctx context.Context, buyerID int64) ([]model.Order, error) {
	var orders []model.Order
	err := r.db.WithContext(ctx).
		Where("buyer_id = ? AND is_deleted = 0", buyerID).
		Order("id DESC").
		Find(&orders).Error
	return orders, err
}

func (r *OrderRepository) ListBySeller(ctx context.Context, sellerID int64) ([]model.Order, error) {
	var orders []model.Order
	err := r.db.WithContext(ctx).
		Where("seller_id = ? AND is_deleted = 0", sellerID).
		Order("id DESC").
		Find(&orders).Error
	return orders, err
}

func lockOrderForUpdate(tx *gorm.DB, orderID int64) (model.Order, error) {
	var order model.Order
	err := tx.Clauses(clause.Locking{Strength: "UPDATE"}).
		Where("id = ? AND is_deleted = 0", orderID).
		First(&order).Error
	return order, err
}

func createOrderEvent(tx *gorm.DB, order model.Order, fromStatus, toStatus, eventType, eventBody string) error {
	return tx.Create(&model.OrderEvent{
		OrderID:    order.ID,
		OrderNo:    order.OrderNo,
		FromStatus: fromStatus,
		ToStatus:   toStatus,
		EventType:  eventType,
		EventBody:  eventBody,
	}).Error
}

func (r *OrderRepository) CancelCreated(ctx context.Context, orderID int64, reason string) (*model.Order, bool, error) {
	var order model.Order
	updated := false
	err := r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		var err error
		order, err = lockOrderForUpdate(tx, orderID)
		if err != nil {
			return err
		}
		if order.Status != model.OrderStatusCreated {
			return nil
		}
		now := time.Now()
		result := tx.Model(&model.Order{}).
			Where("id = ? AND status = ?", orderID, model.OrderStatusCreated).
			Updates(map[string]interface{}{
				"status":        model.OrderStatusCancelled,
				"cancelled_at":  now,
				"cancel_reason": reason,
				"update_time":   now,
			})
		if result.Error != nil {
			return result.Error
		}
		updated = result.RowsAffected == 1
		if updated {
			if err := createOrderEvent(tx, order, model.OrderStatusCreated, model.OrderStatusCancelled, "order.cancelled", reason); err != nil {
				return err
			}
			order.Status = model.OrderStatusCancelled
			order.CancelledAt = &now
			order.CancelReason = reason
		}
		return nil
	})
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, false, nil
	}
	return &order, updated, err
}

func (r *OrderRepository) MarkPaid(ctx context.Context, orderID int64, eventBody string) (*model.Order, bool, error) {
	var order model.Order
	updated := false
	err := r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		var err error
		order, err = lockOrderForUpdate(tx, orderID)
		if err != nil {
			return err
		}
		if order.Status != model.OrderStatusCreated {
			return nil
		}
		now := time.Now()
		result := tx.Model(&model.Order{}).
			Where("id = ? AND status = ?", orderID, model.OrderStatusCreated).
			Updates(map[string]interface{}{
				"status":      model.OrderStatusPaid,
				"paid_at":     now,
				"update_time": now,
			})
		if result.Error != nil {
			return result.Error
		}
		updated = result.RowsAffected == 1
		if updated {
			if err := createOrderEvent(tx, order, model.OrderStatusCreated, model.OrderStatusPaid, "payment.paid", eventBody); err != nil {
				return err
			}
			order.Status = model.OrderStatusPaid
			order.PaidAt = &now
		}
		return nil
	})
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, false, nil
	}
	return &order, updated, err
}

func (r *OrderRepository) SettlePaid(ctx context.Context, orderID int64, payload string) (*model.Order, bool, error) {
	var order model.Order
	updated := false
	err := r.db.WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		var err error
		order, err = lockOrderForUpdate(tx, orderID)
		if err != nil {
			return err
		}
		if order.Status != model.OrderStatusPaid {
			return nil
		}
		now := time.Now()
		result := tx.Model(&model.Order{}).
			Where("id = ? AND status = ?", orderID, model.OrderStatusPaid).
			Updates(map[string]interface{}{
				"status":      model.OrderStatusSettled,
				"settled_at":  now,
				"update_time": now,
			})
		if result.Error != nil {
			return result.Error
		}
		updated = result.RowsAffected == 1
		if updated {
			if err := createOrderEvent(tx, order, model.OrderStatusPaid, model.OrderStatusSettled, "trade.order.settled", payload); err != nil {
				return err
			}
			if err := tx.Create(&model.TradeOutbox{
				EventID:    "trade-order-settled-" + order.OrderNo,
				EventType:  "trade.order.settled",
				RoutingKey: "trade.order.settled",
				Payload:    payload,
				Status:     "NEW",
			}).Error; err != nil {
				return err
			}
			order.Status = model.OrderStatusSettled
			order.SettledAt = &now
		}
		return nil
	})
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, false, nil
	}
	return &order, updated, err
}

func (r *OrderRepository) ListPendingOutbox(ctx context.Context, limit int) ([]model.TradeOutbox, error) {
	if limit <= 0 {
		limit = 20
	}
	var events []model.TradeOutbox
	err := r.db.WithContext(ctx).
		Where("status = ? AND (next_retry_at IS NULL OR next_retry_at <= ?)", "NEW", time.Now()).
		Order("id ASC").
		Limit(limit).
		Find(&events).Error
	return events, err
}

func (r *OrderRepository) MarkOutboxSent(ctx context.Context, id int64) error {
	return r.db.WithContext(ctx).Model(&model.TradeOutbox{}).
		Where("id = ? AND status = ?", id, "NEW").
		Updates(map[string]interface{}{"status": "SENT", "update_time": time.Now()}).Error
}

func (r *OrderRepository) MarkOutboxFailed(ctx context.Context, id int64) error {
	now := time.Now()
	return r.db.WithContext(ctx).Model(&model.TradeOutbox{}).
		Where("id = ? AND status = ?", id, "NEW").
		Updates(map[string]interface{}{
			"retry_count":   gorm.Expr("retry_count + 1"),
			"next_retry_at": now.Add(30 * time.Second),
			"update_time":   now,
		}).Error
}

var (
	ErrGoodsNotFound       = errors.New("商品不存在")
	ErrBuyOwnGoods         = errors.New("不能购买自己发布的商品")
	ErrGoodsAlreadyOrdered = errors.New("商品已被下单或售出")
)
