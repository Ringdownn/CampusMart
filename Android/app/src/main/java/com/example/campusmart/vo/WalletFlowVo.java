package com.example.campusmart.vo;

public class WalletFlowVo {
    private Long id;
    private Long userId;
    private Long orderId;
    private String orderNo;
    private String flowNo;
    private String flowType;
    private String amount;
    private String balanceAfter;
    private String remark;
    private String createTime;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getFlowNo() {
        return flowNo;
    }

    public String getFlowType() {
        return flowType;
    }

    public String getAmount() {
        return amount;
    }

    public String getBalanceAfter() {
        return balanceAfter;
    }

    public String getRemark() {
        return remark;
    }

    public String getCreateTime() {
        return createTime;
    }
}
