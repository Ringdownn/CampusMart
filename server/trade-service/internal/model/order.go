package model

import "time"

const (
	OrderStatusCreated   = "CREATED"
	OrderStatusPaid      = "PAID"
	OrderStatusSettled   = "SETTLED"
	OrderStatusCancelled = "CANCELLED"
	OrderStatusRefunded  = "REFUNDED"
)

type Order struct {
	ID           int64      `gorm:"column:id;primaryKey;autoIncrement" json:"orderId"`
	OrderNo      string     `gorm:"column:order_no;size:64;uniqueIndex;not null" json:"orderNo"`
	GoodsID      int64      `gorm:"column:goods_id;index;not null" json:"goodsId"`
	BuyerID      int64      `gorm:"column:buyer_id;index;not null" json:"buyerId"`
	SellerID     int64      `gorm:"column:seller_id;index;not null" json:"sellerId"`
	Amount       string     `gorm:"column:amount;type:decimal(10,2);not null" json:"amount"`
	Status       string     `gorm:"column:status;size:32;index;not null" json:"status"`
	PayExpireAt  time.Time  `gorm:"column:pay_expire_at;not null" json:"payExpireAt"`
	PaidAt       *time.Time `gorm:"column:paid_at" json:"paidAt,omitempty"`
	SettledAt    *time.Time `gorm:"column:settled_at" json:"settledAt,omitempty"`
	CancelledAt  *time.Time `gorm:"column:cancelled_at" json:"cancelledAt,omitempty"`
	CancelReason string     `gorm:"column:cancel_reason;size:255" json:"cancelReason,omitempty"`
	CreateTime   time.Time  `gorm:"column:create_time;autoCreateTime" json:"createTime"`
	UpdateTime   time.Time  `gorm:"column:update_time;autoUpdateTime" json:"updateTime"`
	IsDeleted    int        `gorm:"column:is_deleted;not null;default:0" json:"-"`
}

func (Order) TableName() string {
	return "orders"
}

type OrderEvent struct {
	ID         int64     `gorm:"column:id;primaryKey;autoIncrement"`
	OrderID    int64     `gorm:"column:order_id;index;not null"`
	OrderNo    string    `gorm:"column:order_no;size:64;index;not null"`
	FromStatus string    `gorm:"column:from_status;size:32"`
	ToStatus   string    `gorm:"column:to_status;size:32;not null"`
	EventType  string    `gorm:"column:event_type;size:64;not null"`
	EventBody  string    `gorm:"column:event_body;type:text"`
	CreateTime time.Time `gorm:"column:create_time;autoCreateTime"`
}

func (OrderEvent) TableName() string {
	return "order_events"
}

type TradeOutbox struct {
	ID          int64      `gorm:"column:id;primaryKey;autoIncrement"`
	EventID     string     `gorm:"column:event_id;size:64;uniqueIndex;not null"`
	EventType   string     `gorm:"column:event_type;size:64;not null"`
	RoutingKey  string     `gorm:"column:routing_key;size:128;not null"`
	Payload     string     `gorm:"column:payload;type:text;not null"`
	Status      string     `gorm:"column:status;size:32;not null;default:NEW"`
	RetryCount  int        `gorm:"column:retry_count;not null;default:0"`
	NextRetryAt *time.Time `gorm:"column:next_retry_at"`
	CreateTime  time.Time  `gorm:"column:create_time;autoCreateTime"`
	UpdateTime  time.Time  `gorm:"column:update_time;autoUpdateTime"`
}

func (TradeOutbox) TableName() string {
	return "trade_outbox"
}

type Goods struct {
	GoodID        int64  `gorm:"column:goodID"`
	PublishUserID int64  `gorm:"column:publishUserID"`
	Title         string `gorm:"column:title"`
	Price         int64  `gorm:"column:price"`
}

func (Goods) TableName() string {
	return "goods"
}
