package org.example.CampusMart.web.controller.login;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.CampusMart.common.login.LoginUser;
import org.example.CampusMart.common.login.LoginUserHolder;
import org.example.CampusMart.common.result.Result;
import org.example.CampusMart.model.entity.User;
import org.example.CampusMart.web.service.UserService;
import org.example.CampusMart.web.vo.LoginVo;
import org.example.CampusMart.web.vo.RegisterVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "登录管理")
@RestController
@RequestMapping("/app")
public class LoginController {
    @Autowired
    private UserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginVo loginVo) {
        String jwt = userService.login(loginVo);
        return Result.ok(jwt);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result register(@RequestBody RegisterVo registerVo) {
        userService.register(registerVo);
        return Result.ok();
    }


    @Operation(summary = "获取登录用户信息")
    @GetMapping("/info")
    public Result<User> getLoginUserInfo() {
        Long id = LoginUserHolder.getLoginUser().getUserId();
        User user = userService.getById(id);
        return Result.ok(user);
    }
}