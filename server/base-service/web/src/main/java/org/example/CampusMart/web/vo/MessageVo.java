package org.example.CampusMart.web.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "聊天详情")
public class MessageVo{
    @Schema(description="messageID")
    private Long messageID;

    @Schema(description="senderID")
    private Long senderID;

    @Schema(description = "sender's nickname")
    private String senderNickname;

    @Schema(description = "sender's avatar url")
    private String senderAvatarURL;

    @Schema(description="receiverID")
    private Long receiverID;

    @Schema(description = "receiver's nickname")
    private String receiverNickname;

    @Schema(description = "receiver's avatar url")
    private String receiverAvatarURL;

    @Schema(description="content")
    private String messageContent;

    @Schema(description="message time")
    private Date sendTime;
}
