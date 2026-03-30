package org.example.CampusMart.web.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Param;
import org.example.CampusMart.model.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.CampusMart.web.vo.MessageVo;
import org.example.CampusMart.web.vo.RecentChatVo;

import java.util.List;

/**
* @author a32271
* @description 针对表【message】的数据库操作Mapper
* @createDate 2025-12-27 23:08:03
* @Entity org.example.CampusMart.model.entity.Message
*/
public interface MessageMapper extends BaseMapper<Message> {

    List<MessageVo> selectMessageListWithUser(@Param("ew") LambdaQueryWrapper<Message> queryWrapper);

    List<RecentChatVo> selectRecentChats(@Param("userId") Long userId);
}




