package com.example.campusmart.entity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Message {
    private Long messageID;
    private Long senderID;
    private Long receiverID;
    private Long goodID;
    private String messageContent;
    private String sendTime;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);

    public Message(Long senderID, Long receiverID, Long goodID, String messageContent) {
        this.senderID = senderID;
        this.receiverID = receiverID;
        this.goodID = goodID;
        this.messageContent = messageContent;
        this.sendTime = DATE_FORMAT.format(new Date());
    }

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

    public Long getReceiverID() {
        return receiverID;
    }

    public void setReceiverID(Long receiverID) {
        this.receiverID = receiverID;
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

    public String getSendTime() {
        return sendTime;
    }

    public void setSendTime(String sendTime) {
        this.sendTime = sendTime;
    }
}