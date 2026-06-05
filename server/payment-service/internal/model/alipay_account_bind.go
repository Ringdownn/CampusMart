package model

import "time"

type AlipayAccountBind struct {
	ID            int64     `gorm:"column:id;primaryKey;autoIncrement" json:"id"`
	UserID        int64     `gorm:"column:user_id;uniqueIndex;not null" json:"userId"`
	AlipayUserID  string    `gorm:"column:alipay_user_id;size:128;not null" json:"alipayUserId"`
	AlipayLoginID string    `gorm:"column:alipay_login_id;size:128" json:"alipayLoginId"`
	Nickname      string    `gorm:"column:nickname;size:64" json:"nickname"`
	BindTime      time.Time `gorm:"column:bind_time;not null" json:"bindTime"`
	CreateTime    time.Time `gorm:"column:create_time;autoCreateTime" json:"createTime"`
	UpdateTime    time.Time `gorm:"column:update_time;autoUpdateTime" json:"updateTime"`
	IsDeleted     int       `gorm:"column:is_deleted;not null;default:0" json:"-"`
}

func (AlipayAccountBind) TableName() string {
	return "alipay_account_binds"
}

type AlipayBindResponse struct {
	Bound         bool   `json:"bound"`
	AlipayUserID  string `json:"alipayUserId,omitempty"`
	AlipayLoginID string `json:"alipayLoginId,omitempty"`
	Nickname      string `json:"nickname,omitempty"`
}
