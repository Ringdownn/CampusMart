package model

import "time"

const (
	FlowTypeEscrowIn     = "ESCROW_IN"
	FlowTypeSellerIncome = "SELLER_INCOME"
	FlowTypeWithdrawOut  = "WITHDRAW_OUT"
	FlowTypeRefundOut    = "REFUND_OUT"
)

type UserWallet struct {
	ID              int64     `gorm:"column:id;primaryKey;autoIncrement" json:"id"`
	UserID          int64     `gorm:"column:user_id;uniqueIndex;not null" json:"userId"`
	AvailableAmount string    `gorm:"column:available_amount;type:decimal(10,2);not null;default:0.00" json:"availableAmount"`
	FrozenAmount    string    `gorm:"column:frozen_amount;type:decimal(10,2);not null;default:0.00" json:"frozenAmount"`
	CreateTime      time.Time `gorm:"column:create_time;autoCreateTime" json:"createTime"`
	UpdateTime      time.Time `gorm:"column:update_time;autoUpdateTime" json:"updateTime"`
	IsDeleted       int       `gorm:"column:is_deleted;not null;default:0" json:"-"`
}

func (UserWallet) TableName() string {
	return "user_wallets"
}

type WalletFlow struct {
	ID           int64     `gorm:"column:id;primaryKey;autoIncrement" json:"id"`
	UserID       int64     `gorm:"column:user_id;index;not null" json:"userId"`
	OrderID      *int64    `gorm:"column:order_id;index;uniqueIndex:uk_order_flow_type,priority:1" json:"orderId,omitempty"`
	OrderNo      *string   `gorm:"column:order_no;size:64" json:"orderNo,omitempty"`
	FlowNo       string    `gorm:"column:flow_no;size:64;uniqueIndex;not null" json:"flowNo"`
	FlowType     string    `gorm:"column:flow_type;size:32;not null;uniqueIndex:uk_order_flow_type,priority:2" json:"flowType"`
	Amount       string    `gorm:"column:amount;type:decimal(10,2);not null" json:"amount"`
	BalanceAfter string    `gorm:"column:balance_after;type:decimal(10,2);not null" json:"balanceAfter"`
	Remark       string    `gorm:"column:remark;size:255" json:"remark,omitempty"`
	CreateTime   time.Time `gorm:"column:create_time;autoCreateTime" json:"createTime"`
	IsDeleted    int       `gorm:"column:is_deleted;not null;default:0" json:"-"`
}

func (WalletFlow) TableName() string {
	return "wallet_flows"
}
