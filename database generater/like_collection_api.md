# 点赞和收藏功能 API 接口文档

## 一、点赞功能

### 1.1 点赞/取消点赞
- **接口路径**: `POST /app/like/toggle`
- **描述**: 用户对商品进行点赞或取消点赞
- **参数**:
  - `userID` (Long, required): 用户ID
  - `goodID` (Long, required): 商品ID
- **返回示例**:
```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": {
    "success": true,
    "likeCount": 10,
    "isLiked": true
  }
}
```

### 1.2 获取商品点赞数
- **接口路径**: `GET /app/like/count`
- **描述**: 获取指定商品的点赞数量
- **参数**:
  - `goodID` (Long, required): 商品ID
- **返回示例**:
```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": 10
}
```

### 1.3 检查用户是否点赞
- **接口路径**: `GET /app/like/check`
- **描述**: 检查指定用户是否已点赞指定商品
- **参数**:
  - `userID` (Long, required): 用户ID
  - `goodID` (Long, required): 商品ID
- **返回示例**:
```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": true
}
```

---

## 二、收藏功能

### 2.1 收藏/取消收藏
- **接口路径**: `POST /app/collection/toggle`
- **描述**: 用户对商品进行收藏或取消收藏
- **参数**:
  - `userID` (Long, required): 用户ID
  - `goodID` (Long, required): 商品ID
- **返回示例**:
```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": {
    "success": true,
    "collectionCount": 5,
    "isCollected": true
  }
}
```

### 2.2 获取商品收藏数
- **接口路径**: `GET /app/collection/count`
- **描述**: 获取指定商品的收藏数量
- **参数**:
  - `goodID` (Long, required): 商品ID
- **返回示例**:
```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": 5
}
```

### 2.3 检查用户是否收藏
- **接口路径**: `GET /app/collection/check`
- **描述**: 检查指定用户是否已收藏指定商品
- **参数**:
  - `userID` (Long, required): 用户ID
  - `goodID` (Long, required): 商品ID
- **返回示例**:
```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": true
}
```

---

## 三、数据库表结构

### 3.1 点赞表 (goods_like)
```sql
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
```

### 3.2 收藏表 (goods_collection)
```sql
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
```

---

## 四、业务逻辑说明

### 4.1 Toggle 机制
- 点赞/收藏使用 **Toggle（切换）** 机制
- 用户已点赞 → 点击取消点赞
- 用户未点赞 → 点击添加点赞
- 同理适用于收藏功能

### 4.2 数据一致性
- 使用唯一索引 `uk_user_good` 保证同一用户对同一商品只能有一条记录
- 支持用户多次切换操作，数据始终保持一致

### 4.3 性能优化
- 为 `goodID` 和 `userID` 添加索引，加快查询速度
- 使用 MyBatis-Plus 提供的快捷方法，简化开发
