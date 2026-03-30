package org.example.CampusMart.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.CampusMart.common.exception.CampusMartException;
import org.example.CampusMart.common.result.ResultCodeEnum;
import org.example.CampusMart.common.utils.CodeUtil;
import org.example.CampusMart.common.utils.JwtUtil;
import org.example.CampusMart.model.entity.User;
import org.example.CampusMart.web.service.PictureService;
import org.example.CampusMart.web.service.UserService;
import org.example.CampusMart.web.mapper.UserMapper;
import org.example.CampusMart.web.vo.LoginVo;
import org.example.CampusMart.web.vo.RegisterVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
* @author a32271
* @description 针对表【user】的数据库操作Service实现
* @createDate 2025-12-27 23:08:03
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Autowired
    private UserMapper userMapper;

    @Value("${minio.endpoint}")
    private String endpoint;
    @Autowired
    private PictureService pictureService;

    @Override
    public boolean register(RegisterVo registerVo) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registerVo.getUsername());
        User user = userMapper.selectOne(queryWrapper);
        if (user!=null) {
            throw new CampusMartException(ResultCodeEnum.ADMIN_ACCOUNT_EXIST_ERROR);
        }
        user = new User();
        BeanUtils.copyProperties(registerVo,user);
        user.setNickname("User"+ CodeUtil.geyRandomCode(5));
        user.setProfileSignature("hello everyone");
        try {
            ClassPathResource resource = new ClassPathResource("default_avatar.jpeg");
            InputStream inputStream = resource.getInputStream();

            MultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    "default_avatar.jpeg",
                    "image/jpeg",
                    inputStream
            );

            // 3. 调用 upload 方法上传
            String url = pictureService.upload(multipartFile);
            user.setAvatarURL(url);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("读取默认头像文件失败", e);
        }
        userMapper.insert(user);
        return true;
    }

    @Override
    public String login(LoginVo loginVo) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, loginVo.getUsername());
        User user = userMapper.selectOne(queryWrapper);
        if (user==null) {
            throw new CampusMartException(ResultCodeEnum.ADMIN_ACCOUNT_NOT_EXIST_ERROR);
        }
        if (!user.getPassword().equals(loginVo.getPassword())) {
            throw new CampusMartException(ResultCodeEnum.ADMIN_ACCOUNT_ERROR);
        }

        return JwtUtil.createToken(user.getUserID(), user.getUsername());
    }
}




