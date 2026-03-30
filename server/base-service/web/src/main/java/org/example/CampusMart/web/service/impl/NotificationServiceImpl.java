package org.example.CampusMart.web.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.CampusMart.model.entity.Notification;
import org.example.CampusMart.web.service.NotificationService;
import org.example.CampusMart.web.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
* @author a32271
* @description 针对表【notification】的数据库操作Service实现
* @createDate 2025-12-27 23:08:03
*/
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification>
    implements NotificationService{
}




