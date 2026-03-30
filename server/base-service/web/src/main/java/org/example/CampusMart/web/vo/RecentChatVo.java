package org.example.CampusMart.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "对话列表")
public class RecentChatVo {
    @Schema(description = "Other's nickname")
    private String otherNickname;

    @Schema(description = "Other's ID")
    private String otherID;

    @Schema(description = "Other's avatar")
    private String otherAvatarURL;

    @Schema(description = "lastest message")
    private String lastestMessage;

    @Schema(description = "lastest message time")
    private Date lastestMessageTime;
}
