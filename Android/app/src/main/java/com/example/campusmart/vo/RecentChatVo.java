package com.example.campusmart.vo;

import java.util.Date;

public class RecentChatVo {
    private String otherNickname;
    private String otherID;
    private String otherAvatarURL;
    private Long goodID;
    private String goodTitle;
    private String goodPictureURL;
    private String lastestMessage;
    private Long lastestMessageTime;

    public String getLastestMessage() {
        return lastestMessage;
    }

    public void setLastestMessage(String lastestMessage) {
        this.lastestMessage = lastestMessage;
    }

    public String getOtherNickname() {
        return otherNickname;
    }

    public void setOtherNickname(String otherNickname) {
        this.otherNickname = otherNickname;
    }

    public String getOtherID() {
        return otherID;
    }

    public void setOtherID(String otherID) {
        this.otherID = otherID;
    }

    public String getOtherAvatarURL() {
        return otherAvatarURL;
    }

    public void setOtherAvatarURL(String otherAvatarURL) {
        this.otherAvatarURL = otherAvatarURL;
    }

    public Long getGoodID() {
        return goodID;
    }

    public void setGoodID(Long goodID) {
        this.goodID = goodID;
    }

    public String getGoodTitle() {
        return goodTitle;
    }

    public void setGoodTitle(String goodTitle) {
        this.goodTitle = goodTitle;
    }

    public String getGoodPictureURL() {
        return goodPictureURL;
    }

    public void setGoodPictureURL(String goodPictureURL) {
        this.goodPictureURL = goodPictureURL;
    }

    public Date getLastestMessageTime() {
        return lastestMessageTime == null ? null : new Date(lastestMessageTime);
    }

    public void setLastestMessageTime(Long lastestMessageTime) {
        this.lastestMessageTime = lastestMessageTime;
    }
}
