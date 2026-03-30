package org.example.CampusMart.web.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.example.CampusMart.model.entity.Goods;
import com.baomidou.mybatisplus.extension.service.IService;
import org.example.CampusMart.web.vo.GoodsVo;

/**
* @author a32271
* @description 针对表【goods】的数据库操作Service
* @createDate 2025-12-27 23:08:03
*/
public interface GoodsService extends IService<Goods> {

    IPage<GoodsVo> pageGoods(IPage<GoodsVo> page);

    IPage<GoodsVo> searchGoodsByTitle(IPage<GoodsVo> page, LambdaQueryWrapper<Goods> queryWrapper);

    IPage<GoodsVo> searchGoodsByPublisherId(IPage<GoodsVo> page, LambdaQueryWrapper<Goods> queryWrapper);
}
