package model

import "time"

type MessageRecord struct {
	MessageID      int64     `gorm:"column:messageID;primaryKey;autoIncrement"`
	SenderID       int64     `gorm:"column:senderID;not null"`
	ReceiverID     int64     `gorm:"column:receiverID;not null"`
	GoodID         int64     `gorm:"column:goodID;not null"`
	MessageContent string    `gorm:"column:message_content;type:text;not null"`
	SendTime       time.Time `gorm:"column:sendTime;not null"`
}

func (MessageRecord) TableName() string {
	return "message"
}
