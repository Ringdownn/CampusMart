package org.example.CampusMart.web.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.CampusMart.model.entity.GoodsLike;

public interface GoodsLikeService extends IService<GoodsLike> {

    Integer getLikeCount(Long goodID);

    Boolean hasUserLiked(Long userID, Long goodID);

    Boolean toggleLike(Long userID, Long goodID);
}
