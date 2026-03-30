package org.example.CampusMart.web.service;

import org.example.CampusMart.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
* @author a32271
* @description 针对表【picture】的数据库操作Service
* @createDate 2025-12-27 23:08:03
*/
public interface PictureService extends IService<Picture> {

    String upload(MultipartFile file);
}
