package service

import (
	"context"
	"log"
	"net/url"
	"strings"

	"campusmart/message-service-go/model"

	"gorm.io/gorm"
)

type MessageService struct {
	db            *gorm.DB
	minioEndpoint string
	minioBucket   string
	publicBaseURL string
	logger        *log.Logger
}

func NewMessageService(db *gorm.DB, minioEndpoint, minioBucket, publicBaseURL string, logger *log.Logger) *MessageService {
	return &MessageService{
		db:            db,
		minioEndpoint: minioEndpoint,
		minioBucket:   minioBucket,
		publicBaseURL: strings.TrimRight(publicBaseURL, "/"),
		logger:        logger,
	}
}

func (s *MessageService) buildAvatarUrl(filename string) string {
	objectName := s.toObjectName(filename)
	if objectName == "" {
		return ""
	}

	if s.publicBaseURL == "" && (strings.HasPrefix(filename, "http://") || strings.HasPrefix(filename, "https://")) {
		return filename
	}
	if s.publicBaseURL == "" {
		return strings.TrimRight(s.minioEndpoint, "/") + "/" + s.minioBucket + "/" + objectName
	}
	return s.publicBaseURL + "/app/files/" + encodeObjectName(objectName)
}

func (s *MessageService) toObjectName(value string) string {
	value = strings.TrimSpace(value)
	if value == "" {
		return ""
	}
	if !strings.HasPrefix(value, "http://") && !strings.HasPrefix(value, "https://") {
		return strings.TrimLeft(value, "/")
	}

	parsed, err := url.Parse(value)
	if err != nil {
		return value
	}
	path := strings.TrimLeft(parsed.Path, "/")
	bucketPrefix := strings.Trim(s.minioBucket, "/") + "/"
	if strings.HasPrefix(path, bucketPrefix) {
		return strings.TrimPrefix(path, bucketPrefix)
	}
	return value
}

func encodeObjectName(objectName string) string {
	parts := strings.Split(strings.TrimLeft(objectName, "/"), "/")
	for i, part := range parts {
		parts[i] = url.PathEscape(part)
	}
	return strings.Join(parts, "/")
}

func (s *MessageService) ListByConversation(ctx context.Context, senderID, receiverID, goodID int64) ([]model.MessageVo, error) {
	var recs []model.MessageRecord
	if err := s.db.WithContext(ctx).
		Where("goodID = ? AND ((senderID = ? AND receiverID = ?) OR (senderID = ? AND receiverID = ?))", goodID, senderID, receiverID, receiverID, senderID).
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
			SenderAvatarURL:   s.buildAvatarUrl(""),
			ReceiverID:        r.ReceiverID,
			ReceiverNickname:  "",
			ReceiverAvatarURL: s.buildAvatarUrl(""),
			GoodID:            r.GoodID,
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
		GoodID            int64
		GoodTitle         string
		GoodPictureURL    string
		LastMessage       string
		LastMessageTime   int64
	}

	query := "SELECT\n" +
		"    dialog.goodID AS good_id,\n" +
		"    COALESCE(g.title, '') AS good_title,\n" +
		"    COALESCE(pic.pictureURL, '') AS good_picture_url,\n" +
		"    otherUser.userID AS other_user_id,\n" +
		"    otherUser.Nickname AS other_user_nickname,\n" +
		"    otherUser.avatarURL AS other_user_avatar,\n" +
		"    latestMsg.message_content AS last_message,\n" +
		"    UNIX_TIMESTAMP(latestMsg.sendTime) * 1000 AS last_message_time\n" +
		"FROM\n" +
		"    (SELECT\n" +
		"         goodID,\n" +
		"         LEAST(senderID, receiverID) AS userX,\n" +
		"         GREATEST(senderID, receiverID) AS userY,\n" +
		"         MAX(messageID) AS latestMessageID\n" +
		"     FROM message\n" +
		"     WHERE senderID = ? OR receiverID = ?\n" +
		"     GROUP BY goodID, userX, userY) AS dialog\n" +
		"        JOIN message latestMsg\n" +
		"             ON latestMsg.messageID = dialog.latestMessageID\n" +
		"        JOIN `user` otherUser\n" +
		"             ON otherUser.userID = (CASE\n" +
		"                                        WHEN dialog.userX = ? THEN dialog.userY\n" +
		"                                        ELSE dialog.userX\n" +
		"                 END)\n" +
		"        LEFT JOIN goods g ON g.goodID = dialog.goodID\n" +
		"        LEFT JOIN (SELECT goodID, MIN(pictureURL) AS pictureURL FROM picture GROUP BY goodID) pic\n" +
		"             ON pic.goodID = dialog.goodID\n" +
		"ORDER BY latestMsg.sendTime DESC, latestMsg.messageID DESC"

	var rows []row
	if err := s.db.WithContext(ctx).Raw(query, userID, userID, userID).Scan(&rows).Error; err != nil {
		return nil, err
	}

	out := make([]model.RecentChatVo, 0, len(rows))
	for _, r := range rows {
		out = append(out, model.RecentChatVo{
			OtherNickname:      r.OtherUserNickname,
			OtherID:            int64ToString(r.OtherUserID),
			OtherAvatarURL:     s.buildAvatarUrl(r.OtherUserAvatar),
			GoodID:             r.GoodID,
			GoodTitle:          r.GoodTitle,
			GoodPictureURL:     s.buildAvatarUrl(r.GoodPictureURL),
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
