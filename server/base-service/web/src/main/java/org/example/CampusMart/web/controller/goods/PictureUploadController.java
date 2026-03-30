package org.example.CampusMart.web.controller.goods;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.minio.errors.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.CampusMart.common.result.Result;
import org.example.CampusMart.model.entity.Picture;
import org.example.CampusMart.model.entity.User;
import org.example.CampusMart.web.service.PictureService;
import org.example.CampusMart.web.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


@Tag(name = "文件管理")
@RequestMapping("/app/goods/file")
@RestController
public class PictureUploadController {

    @Autowired
    private PictureService pictureService;

    @Autowired
    private UserService userService;

    @Operation(summary = "上传商品图片")
    @PostMapping("uploadGoodsPicture")
    public Result<String> uploadGoodsPicture(@RequestParam MultipartFile file,@RequestParam long GoodID){
        LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Picture::getGoodID,GoodID);
        Picture picture = pictureService.getOne(wrapper);
        if(picture!=null){
            pictureService.remove(wrapper);
        }
        picture = new Picture();
        String url = pictureService.upload(file);
        picture.setPictureURL(url);
        picture.setGoodID(GoodID);
        pictureService.save(picture);
        return Result.ok(url);
    }

    @Operation(summary = "上传头像")
    @PostMapping("uploadAvatar")
    public Result<String> upload(@RequestParam MultipartFile file,@RequestParam long userID){
//        LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(Picture::getUserID,userID);
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getUserID,userID);
        User user = userService.getOne(userWrapper);
//        Picture picture = pictureService.getOne(wrapper);
//        if(picture!=null){
//            pictureService.remove(wrapper);
//        }
//        picture = new Picture();
        String url = pictureService.upload(file);
//        picture.setPictureURL(url);
//        picture.setUserID(userID);
        user.setAvatarURL(url);
        userService.updateById(user);
        return Result.ok(url);
    }

}
