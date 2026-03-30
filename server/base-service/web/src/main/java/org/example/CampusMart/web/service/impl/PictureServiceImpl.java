package org.example.CampusMart.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.minio.*;
import org.example.CampusMart.common.exception.CampusMartException;
import org.example.CampusMart.common.minio.MinioProperties;
import org.example.CampusMart.common.result.ResultCodeEnum;
import org.example.CampusMart.model.entity.Picture;
import org.example.CampusMart.web.service.PictureService;
import org.example.CampusMart.web.mapper.PictureMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
* @author a32271
* @description 针对表【picture】的数据库操作Service实现
* @createDate 2025-12-27 23:08:03
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @Override
    public String upload(MultipartFile file) {
        try{
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
            if(!bucketExists){
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .build());
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .config(createBucketPolicyConfig(minioProperties.getBucketName()))
                                .build());
            }
            String filename = new SimpleDateFormat("yyyyMMdd").format(new Date()) +
                    "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .object(filename)
                            .contentType(file.getContentType())
                            .build());
            String url = String.join("/", minioProperties.getEndpoint(), minioProperties.getBucketName(), filename);
            return url;
        }catch(Exception e){
            e.printStackTrace();
            throw new CampusMartException(ResultCodeEnum.DATA_ERROR);
        }
    }

    private String createBucketPolicyConfig(String bucketName) {

        return """
                {
                  "Statement" : [ {
                    "Action" : "s3:GetObject",
                    "Effect" : "Allow",
                    "Principal" : "*",
                    "Resource" : "arn:aws:s3:::%s/*"
                  } ],
                  "Version" : "2012-10-17"
                }
                """.formatted(bucketName);
    }
}




