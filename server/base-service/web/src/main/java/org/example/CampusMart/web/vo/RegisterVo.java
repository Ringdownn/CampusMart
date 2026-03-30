package org.example.CampusMart.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户注册信息")
@Data
public class RegisterVo {
    @Schema(description="username")
    private String username;

    @Schema(description="password")
    private String password;

    @Schema(description="phone")
    private Long phone;

    @Schema(description="school")
    private String schoolName;

    @Schema(description="student id")
    private Long studentID;
}

