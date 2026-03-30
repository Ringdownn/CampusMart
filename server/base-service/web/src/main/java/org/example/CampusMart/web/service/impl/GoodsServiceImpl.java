package org.example.CampusMart.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.CampusMart.model.entity.Goods;
import org.example.CampusMart.web.service.GoodsService;
import org.example.CampusMart.web.mapper.GoodsMapper;
import org.example.CampusMart.web.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
* @author a32271
* @description 针对表【goods】的数据库操作Service实现
* @createDate 2025-12-27 23:08:03
*/
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods>
    implements GoodsService{
    @Autowired
    private GoodsMapper goodsMapper;

    @Override
    public IPage<GoodsVo> pageGoods(IPage<GoodsVo> page) {
        return goodsMapper.selectGoodsPage(page);
    }

    @Override
    public IPage<GoodsVo> searchGoodsByTitle(IPage<GoodsVo> page, LambdaQueryWrapper<Goods> queryWrapper) {
        return goodsMapper.selectGoodsByTitle(page, queryWrapper);
    }

    @Override
    public IPage<GoodsVo> searchGoodsByPublisherId(IPage<GoodsVo> page, LambdaQueryWrapper<Goods> queryWrapper) {
        return goodsMapper.selectGoodsByPublisherId(page,queryWrapper);
    }

}




