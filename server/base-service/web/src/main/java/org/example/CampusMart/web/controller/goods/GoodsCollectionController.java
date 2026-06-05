package org.example.CampusMart.web.controller.goods;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.CampusMart.common.login.LoginUserHolder;
import org.example.CampusMart.common.result.Result;
import org.example.CampusMart.web.service.GoodsCollectionService;
import org.example.CampusMart.web.vo.CollectionResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "收藏管理")
@RestController
@RequestMapping("/app/collection")
public class GoodsCollectionController {

    @Autowired
    private GoodsCollectionService goodsCollectionService;

    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/toggle")
    public Result<CollectionResultVo> toggleCollection(@RequestParam Long goodID) {
        Long userID = LoginUserHolder.getLoginUser().getUserId();
        Boolean success = goodsCollectionService.toggleCollection(userID, goodID);
        return Result.ok(CollectionResultVo.builder()
                .success(success)
                .collectionCount(goodsCollectionService.getCollectionCount(goodID))
                .isCollected(goodsCollectionService.hasUserCollected(userID, goodID))
                .build());
    }

    @Operation(summary = "获取商品收藏数")
    @GetMapping("/count")
    public Result<Integer> getCollectionCount(@RequestParam Long goodID) {
        return Result.ok(goodsCollectionService.getCollectionCount(goodID));
    }

    @Operation(summary = "检查用户是否收藏")
    @GetMapping("/check")
    public Result<Boolean> checkUserCollected(@RequestParam Long goodID) {
        Long userID = LoginUserHolder.getLoginUser().getUserId();
        return Result.ok(goodsCollectionService.hasUserCollected(userID, goodID));
    }
}
