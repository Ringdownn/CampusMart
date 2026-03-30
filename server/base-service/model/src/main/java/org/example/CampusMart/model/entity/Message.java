package org.example.CampusMart.model.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.Date;

@Schema(description = "message")
@Data
@TableName(value =  "message")
@Builder
public class Message {
    @Schema(description="messageID")
    @TableId("messageID")
    private Long messageID;

    @Schema(description="senderID")
    @TableField("senderID")
    private Long senderID;

    @Schema(description="receiverID")
    @TableField("receiverID")
    private Long receiverID;

    @Schema(description="content")
    @TableField("message_content")
    private String messageContent;

    @Schema(description="message time")
    @TableField("sendTime")
    private Date sendTime;
}
