package org.example.CampusMart.web.controller.goods;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.CampusMart.common.login.LoginUserHolder;
import org.example.CampusMart.common.result.Result;
import org.example.CampusMart.web.service.GoodsCollectionService;
import org.example.CampusMart.web.service.GoodsService;
import org.example.CampusMart.web.vo.CollectionResultVo;
import org.example.CampusMart.web.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "收藏管理")
@RestController
@RequestMapping("/app/collection")
public class GoodsCollectionController {

    @Autowired
    private GoodsCollectionService goodsCollectionService;

    @Autowired
    private GoodsService goodsService;

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

    @Operation(summary = "获取用户收藏列表")
    @GetMapping("/list")
    public Result<IPage<GoodsVo>> listCollections(@RequestParam(defaultValue = "1") Long current,
                                                   @RequestParam(defaultValue = "10") Long size) {
        Long userID = LoginUserHolder.getLoginUser().getUserId();
        Page<GoodsVo> page = new Page<>(current, size);
        return Result.ok(goodsService.searchCollectedGoodsByUserId(page, userID));
    }
}
