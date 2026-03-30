package org.example.CampusMart.web.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "商品列表信息")
public class GoodsVo {
    @Schema(description = "picture")
    private String pictureURL;

    @Schema(description="goodID")
    private Long goodID;

    @Schema(description="publishUserID")
    private Long publishUserID;

    @Schema(description = "publishUserAvatar")
    private String avatarURL;

    @Schema(description = "nickname")
    private String nickname;

    @Schema(description="title")
    private String title;

    @Schema(description="appearance")
    private String appearance;

    @Schema(description="item description")
    private String itemDescription;

    @Schema(description="price")
    private Long price;
}
