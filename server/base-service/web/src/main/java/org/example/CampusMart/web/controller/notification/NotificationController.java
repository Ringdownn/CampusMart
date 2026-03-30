package org.example.CampusMart.web.controller.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.CampusMart.common.result.Result;
import org.example.CampusMart.model.entity.Notification;
import org.example.CampusMart.web.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通知管理")
@RestController
@RequestMapping("/app/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @Operation(summary = "分页查询通知")
    @GetMapping("/page")
    public Result<IPage<Notification>> pageNotifications(@RequestParam long current, @RequestParam long size) {
        IPage<Notification> page = new Page<>(current, size);
        LambdaQueryWrapper<Notification> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Notification::getSendTime);
        IPage<Notification> list = notificationService.page(page, queryWrapper);
        return Result.ok(list);
    }
}