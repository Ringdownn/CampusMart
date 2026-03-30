package org.example.CampusMart.model.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "图片")
@Data
@TableName(value =  "picture")
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Picture {
    @Schema(description="图片id")
    @TableId(value = "pictureID",type = IdType.AUTO)
    private Long pictureID;

    @Schema(description="图片对应的url")
    @TableField("pictureURL")
    private String pictureURL;

    @Schema(description="goodID")
    @TableField("goodID")
    private Long goodID;

    @Schema(description="userID")
    @TableField("userID")
    private Long userID;
}
