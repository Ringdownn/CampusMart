package org.example.CampusMart.web.controller.goods;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.CampusMart.common.login.LoginUserHolder;
import org.example.CampusMart.common.result.Result;
import org.example.CampusMart.web.service.GoodsLikeService;
import org.example.CampusMart.web.vo.LikeResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "点赞管理")
@RestController
@RequestMapping("/app/like")
public class GoodsLikeController {

    @Autowired
    private GoodsLikeService goodsLikeService;

    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/toggle")
    public Result<LikeResultVo> toggleLike(@RequestParam Long goodID) {
        Long userID = LoginUserHolder.getLoginUser().getUserId();
        Boolean success = goodsLikeService.toggleLike(userID, goodID);
        return Result.ok(LikeResultVo.builder()
                .success(success)
                .likeCount(goodsLikeService.getLikeCount(goodID))
                .isLiked(goodsLikeService.hasUserLiked(userID, goodID))
                .build());
    }

    @Operation(summary = "获取商品点赞数")
    @GetMapping("/count")
    public Result<Integer> getLikeCount(@RequestParam Long goodID) {
        return Result.ok(goodsLikeService.getLikeCount(goodID));
    }

    @Operation(summary = "检查用户是否点赞")
    @GetMapping("/check")
    public Result<Boolean> checkUserLiked(@RequestParam Long goodID) {
        Long userID = LoginUserHolder.getLoginUser().getUserId();
        return Result.ok(goodsLikeService.hasUserLiked(userID, goodID));
    }
}
