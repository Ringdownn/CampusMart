package org.example.CampusMart.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Date;

@Schema(description = "收藏")
@Data
@TableName(value = "goods_collection")
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class GoodsCollection {
    @Schema(description = "收藏ID")
    @TableId(value = "collectionID", type = IdType.AUTO)
    private Long collectionID;

    @Schema(description = "用户ID")
    @TableField("userID")
    private Long userID;

    @Schema(description = "商品ID")
    @TableField("goodID")
    private Long goodID;

    @Schema(description = "收藏时间")
    @TableField("collectionTime")
    private Date collectionTime;
}
