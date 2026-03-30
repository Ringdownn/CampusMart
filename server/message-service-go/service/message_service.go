package service

import (
	"context"

	"campusmart/message-service-go/model"

	"gorm.io/gorm"
)

type MessageService struct {
	db *gorm.DB
}

func NewMessageService(db *gorm.DB) *MessageService {
	return &MessageService{db: db}
}

func (s *MessageService) ListByUsers(ctx context.Context, senderID, receiverID int64) ([]model.MessageVo, error) {
	var recs []model.MessageRecord
	if err := s.db.WithContext(ctx).
		Where("(senderID = ? AND receiverID = ?) OR (senderID = ? AND receiverID = ?)", senderID, receiverID, receiverID, senderID).
		Order("sendTime asc, messageID asc").
		Find(&recs).Error; err != nil {
		return nil, err
	}

	out := make([]model.MessageVo, 0, len(recs))
	for _, r := range recs {
		out = append(out, model.MessageVo{
			MessageID:         r.MessageID,
			SenderID:          r.SenderID,
			SenderNickname:    "",
			SenderAvatarURL:   "",
			ReceiverID:        r.ReceiverID,
			ReceiverNickname:  "",
			ReceiverAvatarURL: "",
			MessageContent:    r.MessageContent,
			SendTime:          r.SendTime.UnixMilli(),
		})
	}
	return out, nil
}

func (s *MessageService) RecentChats(ctx context.Context, userID int64) ([]model.RecentChatVo, error) {
	type row struct {
		OtherUserID       int64
		OtherUserNickname string
		OtherUserAvatar   string
		LastMessage       string
		LastMessageTime   int64
	}

	query := "SELECT\n" +
		"    otherUser.userID AS other_user_id,\n" +
		"    otherUser.Nickname AS other_user_nickname,\n" +
		"    otherUser.avatarURL AS other_user_avatar,\n" +
		"    latestMsg.message_content AS last_message,\n" +
		"    UNIX_TIMESTAMP(latestMsg.sendTime) * 1000 AS last_message_time\n" +
		"FROM\n" +
		"    (SELECT\n" +
		"         LEAST(senderID, receiverID) AS userX,\n" +
		"         GREATEST(senderID, receiverID) AS userY,\n" +
		"         MAX(sendTime) AS maxSendTime\n" +
		"     FROM message\n" +
		"     WHERE senderID = ? OR receiverID = ?\n" +
		"     GROUP BY userX, userY) AS dialog\n" +
		"        JOIN message latestMsg\n" +
		"             ON ((latestMsg.senderID = dialog.userX AND latestMsg.receiverID = dialog.userY)\n" +
		"                 OR (latestMsg.senderID = dialog.userY AND latestMsg.receiverID = dialog.userX))\n" +
		"                 AND latestMsg.sendTime = dialog.maxSendTime\n" +
		"        JOIN `user` otherUser\n" +
		"             ON otherUser.userID = (CASE\n" +
		"                                        WHEN dialog.userX = ? THEN dialog.userY\n" +
		"                                        ELSE dialog.userX\n" +
		"                 END)\n" +
		"ORDER BY latestMsg.sendTime DESC"

	var rows []row
	if err := s.db.WithContext(ctx).Raw(query, userID, userID, userID).Scan(&rows).Error; err != nil {
		return nil, err
	}

	out := make([]model.RecentChatVo, 0, len(rows))
	for _, r := range rows {
		out = append(out, model.RecentChatVo{
			OtherNickname:      r.OtherUserNickname,
			OtherID:            int64ToString(r.OtherUserID),
			OtherAvatarURL:     r.OtherUserAvatar,
			LastestMessage:     r.LastMessage,
			LastestMessageTime: r.LastMessageTime,
		})
	}
	return out, nil
}

func int64ToString(v int64) string {
	if v == 0 {
		return "0"
	}
	neg := v < 0
	if neg {
		v = -v
	}
	var buf [24]byte
	i := len(buf)
	for v > 0 {
		i--
		buf[i] = byte('0' + v%10)
		v /= 10
	}
	if neg {
		i--
		buf[i] = '-'
	}
	return string(buf[i:])
}
