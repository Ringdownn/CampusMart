package model

type MessageVo struct {
	MessageID         int64  `json:"messageID"`
	SenderID          int64  `json:"senderID"`
	SenderNickname    string `json:"senderNickname"`
	SenderAvatarURL   string `json:"senderAvatarURL"`
	ReceiverID        int64  `json:"receiverID"`
	ReceiverNickname  string `json:"receiverNickname"`
	ReceiverAvatarURL string `json:"receiverAvatarURL"`
	GoodID            int64  `json:"goodID"`
	MessageContent    string `json:"messageContent"`
	SendTime          int64  `json:"sendTime"`
}
