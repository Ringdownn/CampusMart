package org.example.CampusMart.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.CampusMart.model.entity.Message;
import org.example.CampusMart.web.service.MessageService;
import org.example.CampusMart.web.mapper.MessageMapper;
import org.example.CampusMart.web.vo.MessageVo;
import org.example.CampusMart.web.vo.RecentChatVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author a32271
* @description 针对表【message】的数据库操作Service实现
* @createDate 2025-12-27 23:08:03
*/
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
    implements MessageService{
    @Autowired
    private MessageMapper messageMapper;

    @Override
    public List<MessageVo> getMessageListByUsers(LambdaQueryWrapper<Message> queryWrapper) {
        return messageMapper.selectMessageListWithUser(queryWrapper);
    }

    @Override
    public List<RecentChatVo> getRecentChats(Long userId) {
        return messageMapper.selectRecentChats(userId);
    }
}




