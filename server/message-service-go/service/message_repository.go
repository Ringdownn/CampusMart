package service

import (
	"context"
	"time"

	"campusmart/message-service-go/model"
	"gorm.io/gorm"
)

type MessageRepository struct {
	db *gorm.DB
}

func NewMessageRepository(db *gorm.DB) *MessageRepository {
	return &MessageRepository{db: db}
}

func (r *MessageRepository) Insert(ctx context.Context, senderID, receiverID, goodID int64, content string, sendTime time.Time) (int64, error) {
	rec := model.MessageRecord{
		SenderID:       senderID,
		ReceiverID:     receiverID,
		GoodID:         goodID,
		MessageContent: content,
		SendTime:       sendTime,
	}
	if err := r.db.WithContext(ctx).Create(&rec).Error; err != nil {
		return 0, err
	}
	return rec.MessageID, nil
}
