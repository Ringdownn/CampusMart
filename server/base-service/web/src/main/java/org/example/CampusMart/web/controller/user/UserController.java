package org.example.CampusMart.web.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.CampusMart.common.result.Result;
import org.example.CampusMart.model.entity.User;
import org.example.CampusMart.web.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/app/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Operation(summary = "根据用户id修改用户信息")
    @PutMapping("/updateUserInfoByID")
    public Result updateUserInfoByID(@RequestBody User user) {
        boolean result = userService.updateById(user);
        return Result.ok(result);
    }

    @Operation(summary = "根据用户id查询用户头像")
    @GetMapping("/avatar")
    public Result<String> getUserAvatarById(@RequestParam Long id) {
        User user = userService.getById(id);
        String url = user.getAvatarURL();
        return Result.ok(url);
    }
}