package org.example.CampusMart.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.CampusMart.common.minio.MinioProperties;
import org.example.CampusMart.model.entity.Goods;
import org.example.CampusMart.web.service.GoodsService;
import org.example.CampusMart.web.mapper.GoodsMapper;
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
    private MinioProperties minioProperties;

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

    @Override
    public String getGoodsPictureURL(Long goodsId) {
        String filename = goodsMapper.selectGoodsPictureURL(goodsId);
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        return buildPictureUrl(filename);
    }

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        Map<String, Object> userInfo = goodsMapper.selectUserInfo(userId);
        if (userInfo != null && userInfo.get("avatarURL") != null) {
            String filename = (String) userInfo.get("avatarURL");
            userInfo.put("avatarURL", buildPictureUrl(filename));
        }
        return userInfo;
    }

    private String buildPictureUrl(String filename) {
        return String.join("/", minioProperties.getEndpoint(), minioProperties.getBucketName(), filename);
    }

}




