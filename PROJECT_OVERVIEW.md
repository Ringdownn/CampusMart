# 项目介绍文档

## 1. High-level 技术规格

### 目标平台
- Android App + 微服务后端

### 核心技术
- Android 客户端：Android SDK（Java）、AndroidX、Material Design、ConstraintLayout、Fragment/Activity
- 网络与数据：OkHttp、Gson
- 图片加载：Picasso、Glide
- 后端服务：Java（用户/商品/通知服务），Golang（消息/订单/支付服务）
- 通信：REST + WebSocket
- 数据存储：MySQL（主存储）
- 缓存：Redis（商品详情缓存）
- 搜索：MixFound 搜索引擎（独立服务）
- AI 打标：Python + CLIP（商品图片语义标签）
- 消息队列：用于事件驱动、索引更新、订单超时取消
- 部署与运维：Docker / Docker Compose
- 服务注册与发现：Nacos
- 负载均衡与入口层：Nginx

### 高层架构思路
- 微服务拆分：用户服务、商品服务、通知服务（Java）+ 消息服务、订单服务、支付服务（Go）
- 搜索引擎服务与 AI 打标服务独立部署
- 事件驱动：商品变更通过消息队列驱动搜索索引更新
- Redis 作为热点缓存层提升商品详情读取性能

## 2. 开发方法论与适用性

### 方法论
- 迭代式敏捷开发（Scrum/Kanban 混合）

### 适用性理由
- 需求演进快，模块多且边界清晰，适合按服务独立迭代与发布
- 多语言并行开发（Java/Go/Python），可按模块分工并行推进
- 事件驱动架构减少服务耦合，利于持续扩展与性能优化

## 3. 资源需求概览

### 开发工具
- Android Studio（Android）、IntelliJ IDEA（Java）、GoLand/VSCode（Go）、PyCharm/VSCode（Python）
- Postman / Apifox（接口调试）
- Docker / Docker Compose（本地与测试环境部署）

### 外部库与框架
- Android：AndroidX AppCompat、Material、Activity、ConstraintLayout、OkHttp、Gson、Picasso、Glide
- Java：Spring Boot、MyBatis（或 JPA）
- Go：Gin/Fiber、GORM、gorilla/websocket
- Python：FastAPI、PyTorch、CLIP

### 服务与中间件
- MySQL（主存储）
- Redis（缓存）
- 消息队列（RabbitMQ / RocketMQ / Kafka）
- Nacos（服务注册与发现）
- Nginx（负载均衡/网关）

### 搜索与 AI 相关
- MixFound 搜索引擎（Go）：LevelDB、Jieba 分词、索引管理
- AI 打标服务（Python）：OpenAI CLIP 或同类视觉模型
- 数据同步：消息队列事件驱动（商品变更触发索引更新）
