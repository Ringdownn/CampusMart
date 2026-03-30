package org.example.CampusMart.model.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Schema(description = "通知")
@Data
@TableName(value =  "notification")
@Builder
public class Notification {
    @Schema(description="notification id")
    @TableId("notificationID")
    private Long notificationID;

    @Schema(description="sender's name")
    @TableField("senderName")
    private String senderName;

    @Schema(description="content")
    @TableField("notification_content")
    private String notificationContent;

    @Schema(description="send time")
    @TableField("sendTime")
    private Date sendTime;
}
