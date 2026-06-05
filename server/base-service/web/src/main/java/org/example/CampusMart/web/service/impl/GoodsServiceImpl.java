package org.example.CampusMart.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.CampusMart.model.entity.Goods;
import org.example.CampusMart.web.service.GoodsService;
import org.example.CampusMart.web.mapper.GoodsMapper;
import org.example.CampusMart.web.support.MediaUrlBuilder;
import org.example.CampusMart.web.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods>
    implements GoodsService{
    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private MediaUrlBuilder mediaUrlBuilder;

    @Override
    public IPage<GoodsVo> pageGoods(IPage<GoodsVo> page) {
        return withPublicMediaUrls(goodsMapper.selectGoodsPage(page));
    }

    @Override
    public IPage<GoodsVo> searchGoodsByTitle(IPage<GoodsVo> page, LambdaQueryWrapper<Goods> queryWrapper) {
        return withPublicMediaUrls(goodsMapper.selectGoodsByTitle(page, queryWrapper));
    }

    @Override
    public IPage<GoodsVo> searchGoodsByPublisherId(IPage<GoodsVo> page, LambdaQueryWrapper<Goods> queryWrapper) {
        return withPublicMediaUrls(goodsMapper.selectGoodsByPublisherId(page,queryWrapper));
    }

    @Override
    public String getGoodsPictureURL(Long goodsId) {
        String filename = goodsMapper.selectGoodsPictureURL(goodsId);
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        return mediaUrlBuilder.toPublicUrl(filename);
    }

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        Map<String, Object> userInfo = goodsMapper.selectUserInfo(userId);
        if (userInfo != null && userInfo.get("avatarURL") != null) {
            String filename = (String) userInfo.get("avatarURL");
            userInfo.put("avatarURL", mediaUrlBuilder.toPublicUrl(filename));
        }
        return userInfo;
    }

    @Override
    public IPage<GoodsVo> searchCollectedGoodsByUserId(IPage<GoodsVo> page, Long userId) {
        return withPublicMediaUrls(goodsMapper.selectCollectedGoodsByUserId(page, userId));
    }

    private IPage<GoodsVo> withPublicMediaUrls(IPage<GoodsVo> page) {
        if (page == null || page.getRecords() == null) {
            return page;
        }
        page.getRecords().forEach(this::withPublicMediaUrls);
        return page;
    }

    private void withPublicMediaUrls(GoodsVo goodsVo) {
        if (goodsVo == null) {
            return;
        }
        goodsVo.setPictureURL(mediaUrlBuilder.toPublicUrl(goodsVo.getPictureURL()));
        goodsVo.setAvatarURL(mediaUrlBuilder.toPublicUrl(goodsVo.getAvatarURL()));
    }

}




