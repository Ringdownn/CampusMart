package org.example.CampusMart.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Date;

@Schema(description = "点赞")
@Data
@TableName(value = "goods_like")
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class GoodsLike {
    @Schema(description = "点赞ID")
    @TableId(value = "likeID", type = IdType.AUTO)
    private Long likeID;

    @Schema(description = "用户ID")
    @TableField("userID")
    private Long userID;

    @Schema(description = "商品ID")
    @TableField("goodID")
    private Long goodID;

    @Schema(description = "点赞时间")
    @TableField("likeTime")
    private Date likeTime;
}
