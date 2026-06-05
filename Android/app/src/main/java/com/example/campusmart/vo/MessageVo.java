package com.example.campusmart.vo;

public class MessageVo{
    private Long messageID;
    private Long senderID;
    private String senderNickname;
    private String senderAvatarURL;
    private Long receiverID;
    private String receiverNickname;
    private String receiverAvatarURL;
    private Long goodID;
    private String messageContent;
    private Long sendTime;

    public Long getMessageID() {
        return messageID;
    }

    public void setMessageID(Long messageID) {
        this.messageID = messageID;
    }

    public Long getSenderID() {
        return senderID;
    }

    public void setSenderID(Long senderID) {
        this.senderID = senderID;
    }

    public String getSenderNickname() {
        return senderNickname;
    }

    public void setSenderNickname(String senderNickname) {
        this.senderNickname = senderNickname;
    }

    public String getSenderAvatarURL() {
        return senderAvatarURL;
    }

    public void setSenderAvatarURL(String senderAvatarURL) {
        this.senderAvatarURL = senderAvatarURL;
    }

    public Long getReceiverID() {
        return receiverID;
    }

    public void setReceiverID(Long receiverID) {
        this.receiverID = receiverID;
    }

    public String getReceiverNickname() {
        return receiverNickname;
    }

    public void setReceiverNickname(String receiverNickname) {
        this.receiverNickname = receiverNickname;
    }

    public String getReceiverAvatarURL() {
        return receiverAvatarURL;
    }

    public void setReceiverAvatarURL(String receiverAvatarURL) {
        this.receiverAvatarURL = receiverAvatarURL;
    }

    public Long getGoodID() {
        return goodID;
    }

    public void setGoodID(Long goodID) {
        this.goodID = goodID;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public Long getSendTime() {
        return sendTime;
    }

    public void setSendTime(Long sendTime) {
        this.sendTime = sendTime;
    }
}
