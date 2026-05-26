-- 点赞表
CREATE TABLE IF NOT EXISTS `goods_like` (
    `likeID` BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    `userID` BIGINT NOT NULL COMMENT '用户ID',
    `goodID` BIGINT NOT NULL COMMENT '商品ID',
    `likeTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`likeID`),
    UNIQUE KEY `uk_user_good` (`userID`, `goodID`),
    KEY `idx_goodID` (`goodID`),
    KEY `idx_userID` (`userID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品点赞表';

-- 收藏表
CREATE TABLE IF NOT EXISTS `goods_collection` (
    `collectionID` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `userID` BIGINT NOT NULL COMMENT '用户ID',
    `goodID` BIGINT NOT NULL COMMENT '商品ID',
    `collectionTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`collectionID`),
    UNIQUE KEY `uk_user_good` (`userID`, `goodID`),
    KEY `idx_goodID` (`goodID`),
    KEY `idx_userID` (`userID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品收藏表';
