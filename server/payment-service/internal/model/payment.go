package model

import "time"

const (
	PaymentStatusWaitPay = "WAIT_PAY"
	PaymentStatusSuccess = "SUCCESS"
	PaymentStatusClosed  = "CLOSED"

	PaymentChannelAlipaySandbox = "ALIPAY_SANDBOX"
)

type Payment struct {
	ID            int64      `gorm:"column:id;primaryKey;autoIncrement" json:"id"`
	OrderID       int64      `gorm:"column:order_id;uniqueIndex;not null" json:"orderId"`
	OrderNo       string     `gorm:"column:order_no;size:64;index;not null" json:"orderNo"`
	PayNo         string     `gorm:"column:pay_no;size:64;uniqueIndex;not null" json:"payNo"`
	BuyerID       int64      `gorm:"column:buyer_id;index;not null" json:"buyerId"`
	Amount        string     `gorm:"column:amount;type:decimal(10,2);not null" json:"amount"`
	Channel       string     `gorm:"column:channel;size:32;not null;default:ALIPAY_SANDBOX" json:"channel"`
	Status        string     `gorm:"column:status;size:32;not null" json:"status"`
	AlipayTradeNo string     `gorm:"column:alipay_trade_no;size:128" json:"alipayTradeNo,omitempty"`
	NotifyBody    string     `gorm:"column:notify_body;type:text" json:"-"`
	PaidAt        *time.Time `gorm:"column:paid_at" json:"paidAt,omitempty"`
	ClosedAt      *time.Time `gorm:"column:closed_at" json:"closedAt,omitempty"`
	CreateTime    time.Time  `gorm:"column:create_time;autoCreateTime" json:"createTime"`
	UpdateTime    time.Time  `gorm:"column:update_time;autoUpdateTime" json:"updateTime"`
	IsDeleted     int        `gorm:"column:is_deleted;not null;default:0" json:"-"`
}

func (Payment) TableName() string {
	return "payments"
}

type PaymentOutbox struct {
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

func (PaymentOutbox) TableName() string {
	return "payment_outbox"
}
