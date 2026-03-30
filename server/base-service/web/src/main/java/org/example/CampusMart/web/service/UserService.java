package org.example.CampusMart.web.service;

import org.example.CampusMart.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.CampusMart.web.vo.LoginVo;
import org.example.CampusMart.web.vo.RegisterVo;

/**
* @author a32271
* @description 针对表【user】的数据库操作Service
* @createDate 2025-12-27 23:08:03
*/
public interface UserService extends IService<User> {

    boolean register(RegisterVo registerVo);

    String login(LoginVo loginVo);
}
