package model

type RecentChatVo struct {
	OtherNickname      string `json:"otherNickname"`
	OtherID            string `json:"otherID"`
	OtherAvatarURL     string `json:"otherAvatarURL"`
	GoodID             int64  `json:"goodID"`
	GoodTitle          string `json:"goodTitle"`
	GoodPictureURL     string `json:"goodPictureURL"`
	LastestMessage     string `json:"lastestMessage"`
	LastestMessageTime int64  `json:"lastestMessageTime"`
}
