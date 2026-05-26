package org.example.CampusMart.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.CampusMart.model.entity.GoodsCollection;

public interface GoodsCollectionService extends IService<GoodsCollection> {

    Integer getCollectionCount(Long goodID);

    Boolean hasUserCollected(Long userID, Long goodID);

    Boolean toggleCollection(Long userID, Long goodID);
}
