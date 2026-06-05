package com.example.campusmart.entity;

import java.util.Date;

public class Goods {
    private Long goodID;
    private Long publishUserID;
    private String title;
    private String appearance;
    private String itemDescription;
    private Long price;
    private String pictureURL;
    private Date publishTime;

    public Long getGoodID() {
        return goodID;
    }

    public void setGoodID(Long goodID) {
        this.goodID = goodID;
    }

    public Long getPublishUserID() {
        return publishUserID;
    }

    public void setPublishUserID(Long publishUserID) {
        this.publishUserID = publishUserID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAppearance() {
        return appearance;
    }

    public void setAppearance(String appearance) {
        this.appearance = appearance;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public String getPictureURL() {
        return pictureURL;
    }

    public void setPictureURL(String pictureURL) {
        this.pictureURL = pictureURL;
    }

    public Date getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(Date publishTime) {
        this.publishTime = publishTime;
    }
}
