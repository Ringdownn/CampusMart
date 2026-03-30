package org.example.CampusMart.web.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.example.CampusMart.model.entity.Goods;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.CampusMart.web.vo.GoodsVo;

/**
* @author a32271
* @description 针对表【goods】的数据库操作Mapper
* @createDate 2025-12-27 23:08:03
* @Entity org.example.CampusMart.model.entity.Goods
*/
public interface GoodsMapper extends BaseMapper<Goods> {

    IPage<GoodsVo> selectGoodsPage(IPage<GoodsVo> page);

    IPage<GoodsVo> selectGoodsByTitle(IPage<GoodsVo> page, @Param("ew") LambdaQueryWrapper<Goods> queryWrapper);

    IPage<GoodsVo> selectGoodsByPublisherId(IPage<GoodsVo> page, @Param("ew") LambdaQueryWrapper<Goods> queryWrapper);
}




