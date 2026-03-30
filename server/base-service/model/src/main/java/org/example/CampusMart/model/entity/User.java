package org.example.CampusMart.model.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "用户")
@Data
@TableName(value =  "user")
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class User {
    @Schema(description="userID")
    @TableId(value = "userID",type = IdType.NONE)
    private Long userID;

    @Schema(description="username")
    @TableField("username")
    private String username;

    @Schema(description="password")
    @TableField("password")
    private String password;

    @Schema(description="nickname")
    @TableField("Nickname")
    private String nickname;

    @Schema(description="email")
    @TableField("email")
    private String email;

    @Schema(description="phone")
    @TableField("phone")
    private Long phone;

    @Schema(description="profile signature")
        @TableField("profileSignature")
    private String profileSignature;

    @Schema(description="school")
    @TableField("schoolName")
    private String schoolName;

    @Schema(description="student id")
    @TableField("studentID")
    private Long studentID;

    @Schema(description="avatar")
    @TableField("avatarURL")
    private String avatarURL;
}
