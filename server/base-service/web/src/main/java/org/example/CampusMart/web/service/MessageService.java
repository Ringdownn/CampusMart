package org.example.CampusMart.web.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.CampusMart.model.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.CampusMart.web.vo.MessageVo;
import org.example.CampusMart.web.vo.RecentChatVo;

import java.util.List;

/**
* @author a32271
* @description 针对表【message】的数据库操作Service
* @createDate 2025-12-27 23:08:03
*/
public interface MessageService extends IService<Message> {

    List<MessageVo> getMessageListByUsers(LambdaQueryWrapper<Message> queryWrapper);

    List<RecentChatVo> getRecentChats(Long userId);
}
