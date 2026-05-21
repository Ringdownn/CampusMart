# CampusMart API 接口文档

本文档按当前 `CampusMart/deploy/docker/docker-compose.yml` 的运行方式整理。默认对外入口是 API Gateway `http://localhost:8080`，各服务也保留宿主机直连端口用于调试。

## 目录
- [服务概览](#服务概览)
- [通用约定](#通用约定)
- [登录认证](#登录认证)
- [用户管理](#用户管理)
- [商品管理](#商品管理)
- [文件管理](#文件管理)
- [消息管理](#消息管理)
- [通知管理](#通知管理)
- [搜索服务](#搜索服务)
- [AI 打标服务](#ai-打标服务)
- [RabbitMQ 消息契约](#rabbitmq-消息契约)
- [错误码说明](#错误码说明)
- [Curl 测试示例](#curl-测试示例)

---

## 服务概览

### 服务端口

| 服务 | 容器端口 | 宿主机端口 | 说明 |
| --- | --- | --- | --- |
| API Gateway | 8080 | 8080 | 对外统一入口，路由转发和 JWT 鉴权 |
| Base Service | 8081 | 8081 | 登录、用户、商品、文件、通知等业务接口 |
| Message Service | 8083 | 8083 | 消息查询和 WebSocket 实时消息 |
| Search Engine | 5678 | 5678 | 搜索、索引、搜索库管理 |
| AI Tagging | 8084 | 8084 | 图片 AI 打标 HTTP 接口和 RabbitMQ 消费者 |
| MySQL | 3306 | 3307 | 业务数据库 |
| Redis | 6379 | 6379 | 缓存和搜索服务依赖 |
| RabbitMQ | 5672 | 5672 | AMQP 消息队列 |
| RabbitMQ Management | 15672 | 15672 | RabbitMQ 管理页面 |
| Nacos | 8848 | 8848 | 服务注册中心 |
| MinIO | 9000 | 9000 | 对象存储 API |
| MinIO Console | 9001 | 9001 | 对象存储管理页面 |

### 网关路由

| 网关路径 | 转发目标 | 说明 |
| --- | --- | --- |
| `/app/messages/**` | Message Service `http://message-service:8083` | 消息查询接口 |
| `/ws/**` | Message Service `http://message-service:8083` | WebSocket 实时消息 |
| `/api/query` | Search Engine `http://search-engine:5678` | 搜索查询 |
| `/api/index`, `/api/index/**` | Search Engine `http://search-engine:5678` | 索引管理 |
| `/api/search/**` | Search Engine `http://search-engine:5678` | 兼容路径，重写为 `/api/**` |
| `/app/**` | Base Service `lb://base-service` | 登录、用户、商品、文件、通知等接口 |

---

## 通用约定

### Base Service / Message Service 响应格式

```json
{
  "code": 200,
  "message": "成功",
  "data": {}
}
```

### Search Engine 响应格式

```json
{
  "state": true,
  "message": "success",
  "data": {}
}
```

### AI Tagging 响应格式

AI 打标服务使用 FastAPI 原生 JSON 响应。错误时可能返回：

```json
{
  "detail": "Failed to tag image"
}
```

### 认证请求头

需要认证的接口使用：

```http
Authorization: Bearer <jwt>
```

网关会向后端透传：

```http
access-token: <jwt>
X-User-Id: <userId>
X-User-Name: <username>
```

---

## 登录认证

### 1. 用户登录

- **网关接口**: `POST /app/login`
- **直连接口**: `POST http://localhost:8081/app/login`
- **认证**: 不需要
- **请求体**:

```json
{
  "username": "testuser",
  "password": "123456"
}
```

- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### 2. 用户注册

- **网关接口**: `POST /app/register`
- **直连接口**: `POST http://localhost:8081/app/register`
- **认证**: 不需要
- **请求体**:

```json
{
  "username": "newuser",
  "password": "123456",
  "phone": 13900139000,
  "schoolName": "示例大学",
  "studentID": 20260001
}
```

- **说明**: 注册时服务会生成默认昵称、默认签名，并上传默认头像到 MinIO。
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": null
}
```

### 3. 获取当前登录用户信息

- **网关接口**: `GET /app/info`
- **直连接口**: `GET http://localhost:8081/app/info`
- **认证**: 需要
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "userID": 2042215504302575617,
    "username": "testuser",
    "password": "123456",
    "nickname": "User59114",
    "email": null,
    "phone": 13900139003,
    "profileSignature": "hello everyone",
    "schoolName": null,
    "studentID": null,
    "avatarURL": "20260409/default_avatar.jpeg"
  }
}
```

---

## 用户管理

### 1. 修改用户信息

- **网关接口**: `PUT /app/user/updateUserInfoByID`
- **直连接口**: `PUT http://localhost:8081/app/user/updateUserInfoByID`
- **认证**: 需要
- **请求体**:

```json
{
  "userID": 2042215504302575617,
  "nickname": "新昵称",
  "email": "user@example.com",
  "phone": 13900139003,
  "profileSignature": "新的签名",
  "schoolName": "示例大学",
  "studentID": 20260001,
  "avatarURL": "20260409/avatar.jpeg"
}
```

- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": true
}
```

### 2. 查询用户头像

- **网关接口**: `GET /app/user/avatar?id={userID}`
- **直连接口**: `GET http://localhost:8081/app/user/avatar?id={userID}`
- **认证**: 需要
- **参数**:
  - `id`: 用户 ID
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": "20260409/default_avatar.jpeg"
}
```

---

## 商品管理

### 1. 分页查询商品列表

- **网关接口**: `GET /app/goods/page`
- **直连接口**: `GET http://localhost:8081/app/goods/page`
- **认证**: 不需要
- **参数**:
  - `current`: 当前页码
  - `size`: 每页数量
- **示例**: `GET /app/goods/page?current=1&size=10`
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [
      {
        "goodID": 1,
        "publishUserID": 2042215504302575617,
        "title": "二手自行车",
        "appearance": "九成新",
        "itemDescription": "骑行不到100公里",
        "price": 500,
        "pictureURL": "20260409/goods.jpeg",
        "nickname": "卖家昵称",
        "avatarURL": "20260409/avatar.jpeg"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

### 2. 搜索商品

- **网关接口**: `GET /app/goods/search`
- **直连接口**: `GET http://localhost:8081/app/goods/search`
- **认证**: 不需要
- **参数**:
  - `current`: 当前页码
  - `size`: 每页数量
  - `titleKeyword`: 搜索关键词
- **示例**: `GET /app/goods/search?current=1&size=10&titleKeyword=自行车`
- **说明**: 优先调用 Search Engine；失败时回退 MySQL 模糊查询。
- **响应**: 同商品分页。

### 3. 查询商品详情

- **网关接口**: `GET /app/goods/selectById`
- **直连接口**: `GET http://localhost:8081/app/goods/selectById`
- **认证**: 不需要
- **参数**:
  - `id`: 商品 ID
- **示例**: `GET /app/goods/selectById?id=1`
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "goodID": 1,
    "publishUserID": 2042215504302575617,
    "title": "二手自行车",
    "appearance": "九成新",
    "itemDescription": "骑行不到100公里",
    "price": 500,
    "publishTime": "2026-05-21T12:00:00"
  }
}
```

### 4. 添加商品

- **网关接口**: `POST /app/goods/add`
- **直连接口**: `POST http://localhost:8081/app/goods/add`
- **认证**: 需要
- **请求体**:

```json
{
  "publishUserID": 2042215504302575617,
  "title": "二手自行车",
  "appearance": "九成新",
  "itemDescription": "骑行不到100公里",
  "price": 500,
  "publishTime": "2026-05-21T12:00:00"
}
```

- **副作用**: 保存成功后发布 RabbitMQ 索引更新消息到 `index_exchange`，routing key 为 `index.update.base`。
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": 1
}
```

### 5. 修改商品

- **网关接口**: `PUT /app/goods/update`
- **直连接口**: `PUT http://localhost:8081/app/goods/update`
- **认证**: 需要
- **请求体**:

```json
{
  "goodID": 1,
  "publishUserID": 2042215504302575617,
  "title": "二手自行车（降价）",
  "appearance": "九成新",
  "itemDescription": "降价出售",
  "price": 450
}
```

- **副作用**: 更新成功后发布 RabbitMQ 索引更新消息到 `index_exchange`。
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": true
}
```

### 6. 删除商品

- **网关接口**: `PUT /app/goods/deleteById`
- **直连接口**: `PUT http://localhost:8081/app/goods/deleteById`
- **认证**: 需要
- **参数**:
  - `id`: 商品 ID
- **示例**: `PUT /app/goods/deleteById?id=1`
- **副作用**: 调用 Search Engine 删除索引。
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": true
}
```

---

## 文件管理

### 1. 上传商品图片

- **网关接口**: `POST /app/goods/file/uploadGoodsPicture`
- **直连接口**: `POST http://localhost:8081/app/goods/file/uploadGoodsPicture`
- **认证**: 需要
- **Content-Type**: `multipart/form-data`
- **参数**:
  - `file`: 图片文件
  - `GoodID`: 商品 ID
- **副作用**: 上传成功后保存 `picture` 记录，并发布商品索引更新消息。
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": "20260521/uuid-image.jpg"
}
```

### 2. 上传用户头像

- **网关接口**: `POST /app/goods/file/uploadAvatar`
- **直连接口**: `POST http://localhost:8081/app/goods/file/uploadAvatar`
- **认证**: 需要
- **Content-Type**: `multipart/form-data`
- **参数**:
  - `file`: 图片文件
  - `userID`: 用户 ID
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": "20260521/uuid-avatar.jpg"
}
```

---

## 消息管理

当前网关 `/app/messages/**` 转发到 Go Message Service。实时发送消息建议使用 WebSocket；Java Base Service 中保留了一个历史 `POST /app/messages/send`，但当前网关不会转发到该实现。

### 1. 查询两人消息列表

- **网关接口**: `GET /app/messages/list`
- **直连接口**: `GET http://localhost:8083/app/messages/list`
- **认证**: 不需要
- **参数**:
  - `senderId`: 发送人 ID
  - `receiverId`: 接收人 ID
- **示例**: `GET /app/messages/list?senderId=1&receiverId=2`
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "messageID": 1,
      "senderID": 1,
      "senderNickname": "",
      "senderAvatarURL": "",
      "receiverID": 2,
      "receiverNickname": "",
      "receiverAvatarURL": "",
      "messageContent": "你好，这个商品还在吗？",
      "sendTime": 1779336000000
    }
  ]
}
```

### 2. 查询最近对话列表

- **网关接口**: `GET /app/messages/recent`
- **直连接口**: `GET http://localhost:8083/app/messages/recent`
- **认证**: 不需要
- **参数**:
  - `userId`: 当前用户 ID
- **示例**: `GET /app/messages/recent?userId=1`
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "otherNickname": "用户2",
      "otherID": "2",
      "otherAvatarURL": "http://minio:9000/campusmart/avatar.jpeg",
      "lastestMessage": "好的，明天见",
      "lastestMessageTime": 1779336000000
    }
  ]
}
```

### 3. WebSocket 实时消息

- **网关接口**: `ws://localhost:8080/ws?userId={userId}&token={jwt}`
- **直连接口**: `ws://localhost:8083/ws?userId={userId}`
- **认证**:
  - 网关连接需要 `token` 查询参数或 `Authorization: Bearer <jwt>` 请求头。
  - 网关会校验 token，并向消息服务透传 `X-User-Id`；消息服务会校验该用户与 query `userId` 一致。
  - 直连 `8083` 为本地调试兼容模式，未携带 `X-User-Id` 时只校验 `userId` 参数合法。

客户端发送消息 JSON：

```json
{
  "senderID": 1,
  "receiverID": 2,
  "messageContent": "你好，这个商品还在吗？",
  "sendTime": "2026-05-21T12:00:00.000"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `senderID` | number | 否 | 发送人 ID；为空时服务端使用连接的 `userId` |
| `receiverID` | number | 是 | 接收人 ID |
| `messageContent` | string | 是 | 消息内容，服务端会 trim 空白 |
| `sendTime` | string | 否 | 支持 `yyyy-MM-ddTHH:mm:ss.SSS` 或 RFC3339；为空时使用服务端当前时间 |

服务端推送消息 JSON：

```json
{
  "messageID": 1,
  "senderID": 1,
  "receiverID": 2,
  "messageContent": "你好，这个商品还在吗？",
  "sendTime": "2026-05-21T12:00:00.000"
}
```

### 4. 历史兼容：Base Service 发送消息

- **直连接口**: `POST http://localhost:8081/app/messages/send`
- **网关状态**: 当前 `/app/messages/**` 已路由到 Go Message Service，网关不转发到该接口。
- **认证**: 直连时需要 `access-token`
- **请求体**:

```json
{
  "senderID": 1,
  "receiverID": 2,
  "messageContent": "你好",
  "sendTime": "2026-05-21T12:00:00"
}
```

---

## 通知管理

### 1. 分页查询通知

- **网关接口**: `GET /app/notifications/page`
- **直连接口**: `GET http://localhost:8081/app/notifications/page`
- **认证**: 需要
- **参数**:
  - `current`: 当前页码
  - `size`: 每页数量
- **示例**: `GET /app/notifications/page?current=1&size=10`
- **响应**:

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "records": [
      {
        "notificationID": 1,
        "senderName": "系统",
        "notificationContent": "您的商品已上架",
        "sendTime": "2026-05-21T12:00:00"
      }
    ],
    "total": 1,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

## 搜索服务

Search Engine 直连地址为 `http://localhost:5678`。网关暴露 `/api/query`、`/api/index`、`/api/index/**`，并兼容 `/api/search/**`，例如 `/api/search/query` 会重写到 `/api/query`。

### 1. 搜索查询

- **网关接口**: `POST /api/query?database={database}`
- **兼容接口**: `POST /api/search/query?database={database}`
- **直连接口**: `POST http://localhost:5678/api/query?database={database}`
- **认证**: 不需要
- **Query 参数**:
  - `database`: 搜索库名称，例如 `campusmart`
- **请求体**:

```json
{
  "query": "自行车",
  "page": 1,
  "limit": 10,
  "order": "desc",
  "scoreExp": ""
}
```

- **响应**:

```json
{
  "state": true,
  "message": "success",
  "data": {
    "time": 0.123,
    "total": 1,
    "pageCount": 1,
    "page": 1,
    "limit": 10,
    "documents": [
      {
        "id": 1,
        "text": "二手自行车 九成新",
        "imageURL": "20260521/goods.jpeg",
        "tags": ["自行车", "交通工具"],
        "document": {
          "goodID": 1,
          "title": "二手自行车",
          "price": 500
        },
        "score": 10,
        "keys": ["自行车"]
      }
    ],
    "words": ["自行车"]
  }
}
```

### 2. 添加或更新单个索引

- **网关接口**: `POST /api/index?database={database}`
- **兼容接口**: `POST /api/search/index?database={database}`
- **直连接口**: `POST http://localhost:5678/api/index?database={database}`
- **认证**: 不需要
- **Query 参数**:
  - `database`: 搜索库名称
- **请求体**:

```json
{
  "id": 1,
  "text": "二手自行车 九成新",
  "imageURL": "20260521/goods.jpeg",
  "tags": ["二手", "自行车"],
  "document": {
    "goodID": 1,
    "publishUserID": 2042215504302575617,
    "title": "二手自行车",
    "appearance": "九成新",
    "itemDescription": "骑行不到100公里",
    "price": 500,
    "pictureURL": "20260521/goods.jpeg",
    "nickname": "卖家昵称",
    "avatarURL": "20260409/avatar.jpeg"
  }
}
```

- **副作用**: 如果 `imageURL` 非空，Search Engine 会向 `tagging_exchange` 发布 AI 打标任务，同时先写入当前索引。
- **响应**:

```json
{
  "state": true,
  "message": "success"
}
```

### 3. 批量添加或更新索引

- **网关接口**: `POST /api/index/batch?database={database}`
- **兼容接口**: `POST /api/search/index/batch?database={database}`
- **直连接口**: `POST http://localhost:5678/api/index/batch?database={database}`
- **认证**: 不需要
- **请求体**:

```json
[
  {
    "id": 1,
    "text": "二手自行车",
    "document": {
      "goodID": 1,
      "title": "二手自行车"
    }
  },
  {
    "id": 2,
    "text": "二手书籍",
    "document": {
      "goodID": 2,
      "title": "二手书籍"
    }
  }
]
```

- **响应**:

```json
{
  "state": true,
  "message": "success"
}
```

### 4. 删除索引

- **网关接口**: `POST /api/index/remove?database={database}`
- **兼容接口**: `POST /api/search/index/remove?database={database}`
- **直连接口**: `POST http://localhost:5678/api/index/remove?database={database}`
- **认证**: 不需要
- **请求体**:

```json
{
  "id": 1
}
```

- **响应**:

```json
{
  "state": true,
  "message": "success"
}
```

### 5. 搜索库列表

- **直连接口**: `GET http://localhost:5678/api/db/list`
- **网关状态**: 当前网关未显式暴露 `/api/db/**`
- **认证**: 不需要
- **响应**:

```json
{
  "state": true,
  "message": "success",
  "data": ["campusmart"]
}
```

### 6. 创建搜索库

- **直连接口**: `GET http://localhost:5678/api/db/create?database={database}`
- **网关状态**: 当前网关未显式暴露 `/api/db/**`
- **认证**: 不需要
- **参数**:
  - `database`: 搜索库名称
- **响应**:

```json
{
  "state": true,
  "message": "success",
  "data": "create success"
}
```

### 7. 删除搜索库

- **直连接口**: `GET http://localhost:5678/api/db/drop?database={database}`
- **网关状态**: 当前网关未显式暴露 `/api/db/**`
- **认证**: 不需要
- **参数**:
  - `database`: 搜索库名称
- **响应**:

```json
{
  "state": true,
  "message": "success",
  "data": "drop success"
}
```

---

## AI 打标服务

AI Tagging 直连地址为 `http://localhost:8084`。当前网关未显式转发 AI 打标 HTTP 接口。

### 1. 图片打标

- **直连接口**: `POST http://localhost:8084/api/tag`
- **认证**: 不需要
- **请求体**:

```json
{
  "image_url": "http://minio:9000/campusmart/20260521/goods.jpeg",
  "top_k": 5
}
```

- **字段说明**:

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `image_url` | string | 是 | 图片 URL，不能为空 |
| `top_k` | number | 是 | 返回标签数量，范围 1 到 20 |

- **响应**:

```json
{
  "tags": [
    {
      "label": "自行车",
      "score": 0.95
    }
  ],
  "image_url": "http://minio:9000/campusmart/20260521/goods.jpeg",
  "top_k": 1
}
```

### 2. 健康检查

- **直连接口**: `GET http://localhost:8084/health`
- **认证**: 不需要
- **响应**:

```json
{
  "status": "healthy"
}
```

---

## RabbitMQ 消息契约

### 1. 商品索引更新消息

Base Service 在新增商品、修改商品、上传商品图片后发布。

- **Exchange**: `index_exchange`
- **类型**: `direct`
- **Routing Key**: `index.update.base`
- **消费者**: Search Engine `IndexConsumer`
- **消息体**:

```json
{
  "database": "campusmart",
  "source": "base",
  "doc": {
    "id": 123,
    "text": "二手自行车 骑行不到100公里",
    "imageURL": "20260521/goods.jpeg",
    "document": {
      "id": 2042215504302575617,
      "goodID": 2042215504302575617,
      "publishUserID": 2042215504302575617,
      "title": "二手自行车",
      "appearance": "九成新",
      "itemDescription": "骑行不到100公里",
      "price": 500,
      "pictureURL": "20260521/goods.jpeg",
      "nickname": "卖家昵称",
      "avatarURL": "20260409/avatar.jpeg"
    }
  }
}
```

### 2. AI 打标任务消息

Search Engine 在收到带 `imageURL` 的索引文档时发布。

- **Exchange**: `tagging_exchange`
- **Queue**: `tagging_queue`
- **Routing Key**: `tagging.task`
- **消费者**: AI Tagging `TaggingConsumer`
- **消息体**:

```json
{
  "database": "campusmart",
  "doc": {
    "id": 123,
    "text": "二手自行车 骑行不到100公里",
    "imageURL": "http://minio:9000/campusmart/20260521/goods.jpeg",
    "document": {
      "goodID": 2042215504302575617,
      "title": "二手自行车"
    }
  }
}
```

### 3. AI 打标完成后的索引更新消息

AI Tagging 消费 `tagging.task` 后，为文档补充 `tags` 并重新发布索引更新。

- **Exchange**: `index_exchange`
- **Routing Key**: `index.update.ai`
- **消费者**: Search Engine `IndexConsumer`
- **消息体**:

```json
{
  "database": "campusmart",
  "source": "ai",
  "doc": {
    "id": 123,
    "text": "二手自行车 骑行不到100公里",
    "imageURL": "http://minio:9000/campusmart/20260521/goods.jpeg",
    "tags": ["自行车", "交通工具"],
    "document": {
      "goodID": 2042215504302575617,
      "title": "二手自行车"
    }
  }
}
```

---

## 错误码说明

### Base Service / Message Service

| 错误码 | 说明 |
| --- | --- |
| 200 | 成功 |
| 201 | 失败 |
| 202 | 参数不正确 |
| 203 | 服务异常 |
| 204 | 数据异常 |
| 205 | 非法请求 |
| 301 | 账号已存在 |
| 305 | 未登陆 |
| 306 | 账号不存在 |
| 307 | 用户名或密码错误 |
| 601 | token 过期 |
| 602 | token 非法 |

### HTTP 状态码

| 状态码 | 说明 |
| --- | --- |
| 200 | HTTP 请求成功，业务结果看响应体 |
| 400 | 请求参数错误或 WebSocket userId 非法 |
| 401 | 网关未授权 |
| 403 | WebSocket token 用户与 query userId 不一致 |
| 404 | 路径不存在 |
| 500 | 服务内部异常 |
| 503 | 网关无法访问后端服务 |

---

## Curl 测试示例

### 1. 登录并保存 JWT

```bash
TOKEN=$(curl -sS -X POST 'http://localhost:8080/app/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"testuser","password":"123456"}' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"])')
```

### 2. 获取当前用户

```bash
curl -H "Authorization: Bearer $TOKEN" \
  'http://localhost:8080/app/info'
```

### 3. 查询商品列表

```bash
curl 'http://localhost:8080/app/goods/page?current=1&size=10'
```

### 4. 搜索商品

```bash
curl 'http://localhost:8080/app/goods/search?current=1&size=10&titleKeyword=自行车'
```

### 5. 查询消息列表

```bash
curl 'http://localhost:8080/app/messages/list?senderId=1&receiverId=2'
```

### 6. 搜索引擎查询

```bash
curl -X POST 'http://localhost:8080/api/query?database=campusmart' \
  -H 'Content-Type: application/json' \
  -d '{"query":"自行车","page":1,"limit":10}'
```

### 7. AI 图片打标

```bash
curl -X POST 'http://localhost:8084/api/tag' \
  -H 'Content-Type: application/json' \
  -d '{"image_url":"http://minio:9000/campusmart/20260521/goods.jpeg","top_k":5}'
```

### 8. 上传商品图片

```bash
curl -X POST 'http://localhost:8080/app/goods/file/uploadGoodsPicture' \
  -H "Authorization: Bearer $TOKEN" \
  -F 'GoodID=1' \
  -F 'file=@/path/to/image.jpg'
```

---

## 启动与运维

### 启动所有服务

```bash
cd /Users/a32271/GolandProjects/CampusMart/deploy/docker
docker compose up -d --build
```

### 查看服务状态

```bash
docker compose ps
```

### 查看日志

```bash
docker compose logs -f gateway base-service message-service search-engine ai-tagging
```

### 停止服务

```bash
docker compose down
```

---

## 注意事项

1. `Message Service` 的发送消息主链路是 WebSocket；`POST /app/messages/send` 是 Base Service 的历史直连接口，当前网关不会转发到它。
2. Search Engine 的 `/api/db/**` 当前只建议直连 `5678` 调试使用，网关未显式暴露。
3. AI Tagging HTTP 接口当前只建议直连 `8084` 调试使用，线上索引链路通过 RabbitMQ 自动触发。
4. MinIO 文件上传接口返回的是对象 key，例如 `20260521/uuid.jpg`；客户端展示时需要按部署环境拼接公开访问前缀。
5. 需要认证的 `/app/**` 接口统一使用 `Authorization: Bearer <jwt>`；公开商品查询、消息查询、搜索查询可不带 token。

---

## 技术栈

- Spring Boot 3.x / Spring Cloud Gateway / OpenFeign
- Go Gin / Gorilla WebSocket / GORM
- FastAPI / PyTorch / CLIP 图片打标
- MySQL 8.0 / Redis 7 / RabbitMQ 3.12 / Nacos / MinIO
- Docker Compose
