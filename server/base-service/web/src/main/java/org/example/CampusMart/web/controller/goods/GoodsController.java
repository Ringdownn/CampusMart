package org.example.CampusMart.web.controller.goods;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.CampusMart.common.result.Result;
import org.example.CampusMart.model.entity.Goods;
import org.example.CampusMart.web.service.GoodsService;
import org.example.CampusMart.web.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/app/goods")
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @Operation(summary = "分页查询商品列表（时间顺序）")
    @GetMapping("/page")
    public Result<IPage<GoodsVo>> pageGoods(@RequestParam long current, @RequestParam long size) {
        IPage<GoodsVo> page = new Page<>(current, size);
        IPage<GoodsVo> list = goodsService.pageGoods(page);
        return Result.ok(list);
    }

    @Operation(summary = "按标题模糊查询商品列表（时间顺序）")
    @GetMapping("/search")
    public Result<IPage<GoodsVo>> searchGoodsByTitle(@RequestParam long current, @RequestParam long size, @RequestParam String titleKeyword) {
        IPage<GoodsVo> page = new Page<>(current, size);
        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
        if (titleKeyword != null && !titleKeyword.isEmpty()) {
            queryWrapper.like(Goods::getTitle, titleKeyword);
        }
        IPage<GoodsVo> list = goodsService.searchGoodsByTitle(page,queryWrapper);
        return Result.ok(list);
    }

    @Operation(summary = "根据商品ID查询商品详情")
    @GetMapping("/selectById")
    public Result<Goods> getGoodsById(@RequestParam Long id) {
        Goods goods = goodsService.getById(id);
        return Result.ok(goods);
    }

    @Operation(summary = "添加商品")
    @PostMapping("/add")
    public Result addGoods(@RequestBody Goods goods) {
        boolean result = goodsService.save(goods);
        long newGoodsID = goods.getGoodID();
        return Result.ok(newGoodsID);
    }

    @Operation(summary = "修改商品")
    @PutMapping("/update")
    public Result updateGoods(@RequestBody Goods goods) {
        boolean result = goodsService.updateById(goods);
        return Result.ok(result);
    }

    @Operation(summary = "根据id删除商品")
    @PutMapping("/deleteById")
    public Result deleteGoodsById(@RequestParam Long id) {
        boolean result = goodsService.removeById(id);
        return Result.ok(result);
    }

    @Operation(summary = "根据发帖人查询商品列表")
    @GetMapping("/poster")
    public Result<IPage<GoodsVo>> getGoodsByPoster(@RequestParam Long posterId, @RequestParam long current, @RequestParam long size) {
        IPage<GoodsVo> page = new Page<>(current, size);
        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Goods::getPublishUserID, posterId);
        IPage<GoodsVo> list = goodsService.searchGoodsByPublisherId(page,queryWrapper);
        return Result.ok(list);
    }
}