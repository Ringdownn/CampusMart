package org.example.CampusMart.web.controller.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.CampusMart.common.result.Result;
import org.example.CampusMart.model.entity.Message;
import org.example.CampusMart.web.service.MessageService;
import org.example.CampusMart.web.vo.MessageVo;
import org.example.CampusMart.web.vo.RecentChatVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "消息管理")
@RestController
@RequestMapping("/app/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public Result sendMessage(@RequestBody Message message) {
        boolean result = messageService.save(message);
        return Result.ok(result);
    }

    @Operation(summary = "根据发送人和接受人查询消息列表")
    @GetMapping("/list")
    public Result<List<MessageVo>> getMessageListByUsers(@RequestParam Long senderId, @RequestParam Long receiverId) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(w -> w.eq(Message::getSenderID, senderId).eq(Message::getReceiverID, receiverId))
                .or(w -> w.eq(Message::getSenderID, receiverId).eq(Message::getReceiverID, senderId));
        List<MessageVo> list = messageService.getMessageListByUsers(queryWrapper);
        return Result.ok(list);
    }

    @Operation(summary = "根据发送人或接受人查询最近对话列表")
    @GetMapping("/recent")
    public Result<List<RecentChatVo>> getRecentChats(@RequestParam Long userId) {
        List<RecentChatVo> list = messageService.getRecentChats(userId);
        return Result.ok(list);
    }
}