package org.example.CampusMart.model.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Schema(description = "goods")
@Data
@TableName(value =  "goods")
@Builder
public class Goods {
    @Schema(description="goodID")
    @TableId("goodID")
    private Long goodID;

    @Schema(description="publishUserID")
    @TableField("publishUserID")
    private Long publishUserID;

    @Schema(description="title")
    @TableField("title")
    private String title;

    @Schema(description="appearance")
    @TableField("appearance")
    private String appearance;

    @Schema(description="item description")
    @TableField("itemDescription")
    private String itemDescription;

    @Schema(description="price")
    @TableField("price")
    private Long price;

    @Schema(description="publishTime")
    @TableField("publishTime")
    private Date publishTime;
}
