package model

type RecentChatVo struct {
	OtherNickname      string `json:"otherNickname"`
	OtherID            string `json:"otherID"`
	OtherAvatarURL     string `json:"otherAvatarURL"`
	LastestMessage     string `json:"lastestMessage"`
	LastestMessageTime int64  `json:"lastestMessageTime"`
}
