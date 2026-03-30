package org.example.CampusMart.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录信息")
public class LoginVo {

    @Schema(description="用户名")
    private String username;

    @Schema(description="密码")
    private String password;
}
