package model

type Message struct {
	MessageID      *int64 `json:"messageID,omitempty"`
	SenderID       int64  `json:"senderID"`
	ReceiverID     int64  `json:"receiverID"`
	MessageContent string `json:"messageContent"`
	SendTime       string `json:"sendTime,omitempty"`
}

