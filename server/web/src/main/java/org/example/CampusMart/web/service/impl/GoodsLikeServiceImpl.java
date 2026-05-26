package org.example.CampusMart.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.CampusMart.model.entity.GoodsLike;
import org.example.CampusMart.web.mapper.GoodsLikeMapper;
import org.example.CampusMart.web.service.GoodsLikeService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class GoodsLikeServiceImpl extends ServiceImpl<GoodsLikeMapper, GoodsLike> implements GoodsLikeService {

    @Override
    public Integer getLikeCount(Long goodID) {
        return lambdaQuery().eq(GoodsLike::getGoodID, goodID).count().intValue();
    }

    @Override
    public Boolean hasUserLiked(Long userID, Long goodID) {
        return lambdaQuery().eq(GoodsLike::getUserID, userID).eq(GoodsLike::getGoodID, goodID).exists();
    }

    @Override
    public Boolean toggleLike(Long userID, Long goodID) {
        return hasUserLiked(userID, goodID) 
            ? lambdaUpdate().eq(GoodsLike::getUserID, userID).eq(GoodsLike::getGoodID, goodID).remove()
            : save(GoodsLike.builder().userID(userID).goodID(goodID).likeTime(new Date()).build());
    }
}
