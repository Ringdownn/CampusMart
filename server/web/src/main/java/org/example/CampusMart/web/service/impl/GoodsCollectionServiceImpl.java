package org.example.CampusMart.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.CampusMart.model.entity.GoodsCollection;
import org.example.CampusMart.web.mapper.GoodsCollectionMapper;
import org.example.CampusMart.web.service.GoodsCollectionService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class GoodsCollectionServiceImpl extends ServiceImpl<GoodsCollectionMapper, GoodsCollection> implements GoodsCollectionService {

    @Override
    public Integer getCollectionCount(Long goodID) {
        return lambdaQuery().eq(GoodsCollection::getGoodID, goodID).count().intValue();
    }

    @Override
    public Boolean hasUserCollected(Long userID, Long goodID) {
        return lambdaQuery().eq(GoodsCollection::getUserID, userID).eq(GoodsCollection::getGoodID, goodID).exists();
    }

    @Override
    public Boolean toggleCollection(Long userID, Long goodID) {
        return hasUserCollected(userID, goodID) 
            ? lambdaUpdate().eq(GoodsCollection::getUserID, userID).eq(GoodsCollection::getGoodID, goodID).remove()
            : save(GoodsCollection.builder().userID(userID).goodID(goodID).collectionTime(new Date()).build());
    }
}
