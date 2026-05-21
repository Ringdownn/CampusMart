# CampusMart 微服务架构、服务通信与容器化部署现状文档

本文档整合并替代以下旧文档：

- `IMPLEMENTATION_DOC.md`
- `CONTAINER_DEPLOYMENT_IMPLEMENTATION.md`
- `NACOS_IMPLEMENTATION.md`
- `SERVICE_COMMUNICATION_IMPLEMENTATION.md`
- `SERVICE_COMMUNICATION.md`

文档内容按当前代码与 `deploy/docker/docker-compose.yml` 的实际状态整理，重点说明服务职责、通信方式、Nacos 使用情况、RabbitMQ 消息链路和 Docker Compose 部署方式。

---

## 1. 当前整体架构

CampusMart 当前由 Android 客户端、Spring Cloud Gateway、Java 基础业务服务、Go 消息服务、Go 搜索服务、Python AI 打标服务以及 MySQL、Redis、RabbitMQ、Nacos、MinIO 等基础设施组成。

```text
Android 客户端
   |
   | HTTP / WebSocket
   v
Spring Cloud Gateway (:8080)
   |
   | /app/**             -> base-service (:8081, lb://base-service)
   | /ws/**              -> message-service (:8083, MESSAGE_SERVICE_URI)
   | /app/messages/**    -> message-service (:8083, MESSAGE_SERVICE_URI)
   | /api/query          -> search-engine (:5678, SEARCH_ENGINE_URI)
   | /api/index/**       -> search-engine (:5678, SEARCH_ENGINE_URI)
   v
业务服务层
   |
   | base-service        Java Spring Boot，商品/用户/订单/图片等基础业务
   | message-service     Go + Gin + WebSocket，聊天与消息查询
   | search-engine       Go + Gin，全文搜索、索引、打标任务投递
   | ai-tagging          Python + FastAPI + CLIP，图片打标与标签回写
   v
基础设施层
   |
   | MySQL / Redis / RabbitMQ / Nacos / MinIO
```

### 服务清单

| 服务 | 技术栈 | 容器内端口 | 宿主机端口 | 当前职责 |
| --- | --- | ---: | ---: | --- |
| `gateway` | Spring Cloud Gateway | 8080 | 8080 | 统一入口、JWT 鉴权、路由转发、Nacos 注册 |
| `base-service` | Spring Boot | 8081 | 8081 | 商品、用户、订单、图片、搜索调用与索引消息生产 |
| `message-service` | Go + Gin + WebSocket | 8083 | 8083 | WebSocket 聊天、聊天记录、最近联系人、Nacos 注册 |
| `search-engine` | Go + Gin | 5678 | 5678 | 搜索查询、索引增删、消费索引队列、投递打标队列 |
| `ai-tagging` | Python + FastAPI | 8080 | 8084 映射到容器 8084 | 图片打标、消费打标队列、回写索引队列 |
| `mysql` | MySQL 8.0 | 3306 | 3307 | 业务数据与 Nacos 持久化数据 |
| `redis` | Redis 7 | 6379 | 6379 | 缓存、Gateway Redis 配置 |
| `rabbitmq` | RabbitMQ 3.12 | 5672 / 15672 | 5672 / 15672 | 索引与打标异步消息 |
| `nacos` | Nacos 2.2.3 slim | 8848 / 9848 | 8848 / 9848 | Java 服务注册与配置中心 |
| `minio` | MinIO | 9000 / 9001 | 9000 / 9001 | 图片对象存储 |

> 注意：`ai-tagging` 的 Dockerfile 暴露 8080，应用默认监听 `API_PORT=8080`；但当前 Compose 配置写的是 `8084:8084`。如果不额外设置 `API_PORT=8084` 或修改端口映射为 `8084:8080`，宿主机访问 `8084` 可能无法打到应用。

---

## 2. Nacos 当前实现状态

### 已实现

`gateway` 与 `base-service` 是 Spring Cloud 服务，均已启用：

- `@EnableDiscoveryClient`
- Nacos Discovery 依赖
- Nacos Config 依赖，但当前 `application.yml` 中 `spring.cloud.nacos.config.enabled=false`

`message-service` 使用 `nacos-sdk-go/v2`，启动后通过 `registerToNacos` 注册临时实例：

- 服务名：`message-service`
- 默认端口：`8083`
- Nacos 地址：`NACOS_SERVER_ADDR`，默认 `localhost:8848`
- 命名空间：`NACOS_NAMESPACE`，默认 `campusmart`
- 分组：`NACOS_GROUP`，默认 `DEFAULT_GROUP`

### 当前未完全落地

`search-engine` 当前代码中没有 Nacos SDK 注册逻辑，也未通过 Nacos 被 Gateway 动态发现。Gateway 访问搜索服务时使用：

```yaml
SEARCH_ENGINE_URI: ${SEARCH_ENGINE_URI:-http://search-engine:5678}
```

`ai-tagging` 当前代码中没有 Nacos 注册逻辑，主要通过 RabbitMQ 与搜索服务协作，也提供 `/api/tag` 和 `/health` HTTP 接口。

### Gateway 路由现状

`gateway` 当前路由配置如下：

| 路由 | 目标 | 说明 |
| --- | --- | --- |
| `/ws/**` | `${MESSAGE_SERVICE_URI:http://localhost:8083}` | WebSocket 转发到消息服务 |
| `/app/messages/**` | `${MESSAGE_SERVICE_URI:http://localhost:8083}` | 消息查询 API 转发到消息服务 |
| `/api/query`, `/api/index`, `/api/index/**` | `${SEARCH_ENGINE_URI:http://localhost:5678}` | 搜索服务 API |
| `/api/search/**` | `${SEARCH_ENGINE_URI:http://localhost:5678}` + RewritePath | 兼容旧搜索路径 |
| `/app/**` | `lb://base-service` | 通过 Nacos/LoadBalancer 调用基础服务 |

---

## 3. 服务间通信现状

CampusMart 当前同时使用同步 HTTP、异步 RabbitMQ 和 WebSocket。

### 3.1 HTTP / Feign

`base-service` 通过 OpenFeign 调用 `search-engine`：

```java
@FeignClient(name = "search-engine", url = "${search-engine.url:http://localhost:5678}", path = "/api")
public interface SearchEngineClient {
    @PostMapping("/query")
    Map<String, Object> search(@RequestParam("database") String database,
                               @RequestBody Map<String, Object> request);

    @PostMapping("/index")
    Map<String, Object> addIndex(@RequestParam("database") String database,
                                  @RequestParam("collection") String collection,
                                  @RequestBody Map<String, Object> document);

    @PostMapping("/index/remove")
    Map<String, Object> removeIndex(@RequestParam("database") String database,
                                     @RequestParam("collection") String collection,
                                     @RequestBody Map<String, Object> document);
}
```

当前 Feign 客户端虽然声明了 `name = "search-engine"`，但同时配置了固定 URL，因此实际优先按 `search-engine.url` / 默认 `localhost:5678` 访问，而不是通过 Nacos 发现 `search-engine`。

### 3.2 RabbitMQ 异步链路

RabbitMQ 主要承载商品索引和图片打标链路。

```text
base-service
   |
   | index_exchange / index.update.base
   v
search-engine
   |
   | 如果 doc.imageURL 不为空
   | tagging_exchange / tagging.task
   v
ai-tagging
   |
   | 打标完成后补充 tags
   | index_exchange / index.update.ai
   v
search-engine
```

当前队列与交换机：

| 名称 | 类型 | 使用方 | 说明 |
| --- | --- | --- | --- |
| `index_exchange` | direct | `base-service`, `search-engine`, `ai-tagging` | 索引更新交换机 |
| `index_queue` | durable queue | `search-engine` | 消费 `index.update.base` 和 `index.update.ai` |
| `tagging_exchange` | direct | `search-engine`, `ai-tagging` | AI 打标交换机 |
| `tagging_queue` | durable queue | `ai-tagging` | 消费 `tagging.task` |

`base-service` 在商品新增和修改时调用 `SearchEngineService.publishIndexUpdate`，发送结构为：

```json
{
  "database": "campusmart",
  "source": "base",
  "doc": {
    "id": 123,
    "text": "标题 描述",
    "imageURL": "http://...",
    "document": {
      "goodID": 123,
      "title": "...",
      "price": 100,
      "pictureURL": "...",
      "nickname": "...",
      "avatarURL": "..."
    }
  }
}
```

删除商品时当前不是通过 MQ，而是同步 Feign 调用 `POST /api/index/remove`，以便尽快删除搜索索引。

### 3.3 WebSocket 消息服务

`message-service` 独立处理实时聊天，与 `base-service` 不耦合。

接口：

| 接口 | 方法 | 功能 |
| --- | --- | --- |
| `/ws?userId=xxx` | WebSocket GET | 建立实时聊天连接 |
| `/app/messages/list` | GET | 查询两人聊天记录 |
| `/app/messages/recent` | GET | 查询最近联系人 |

Gateway 的 `WebSocketAuthFilter` 会校验 `token` 或 `Authorization`，并转发：

- `X-User-Id`
- `X-User-Name`
- `access-token`

`message-service` 的 WebSocket Handler 会检查查询参数 `userId`，并在存在 `X-User-Id` 时校验它与 `userId` 一致；直连本地服务时仍兼容无 Header 的连接。

---

## 4. 容器化部署现状

部署入口：

```bash
cd deploy/docker
docker compose up -d --build
```

当前 Compose 编排的构建路径：

| 服务 | 构建上下文 | Dockerfile |
| --- | --- | --- |
| `gateway` | `../../server/gateway` | `Dockerfile` |
| `base-service` | `../../server/base-service` | `Dockerfile` |
| `message-service` | `../../server/message-service-go` | `Dockerfile` |
| `search-engine` | `../../server/search-engine` | `Dockerfile` |
| `ai-tagging` | `../../server/ai-tagging` | `Dockerfile` |

### Dockerfile 概览

- `gateway`：`eclipse-temurin:17-jdk` 构建，`eclipse-temurin:17-jre` 运行，端口 8080。
- `base-service`：在 `server/base-service` 根模块中构建 `common,model,web`，端口 8081。
- `message-service`：`golang:1.22-alpine` 构建，`alpine` 运行，端口 8083。
- `search-engine`：`golang:1.26-alpine` 构建 `./cmd/main.go`，复制 `config.yaml`，端口 5678。
- `ai-tagging`：`python:3.12-slim`，安装依赖后运行 `python cmd/main.py`，应用默认端口 8080。

### 当前环境变量要点

`deploy/docker/.env.example` 只包含常用基础项，Compose 中还通过默认值补齐了若干变量：

| 变量 | 当前默认值 | 使用方 |
| --- | --- | --- |
| `MYSQL_ROOT_PASSWORD` | `root` | MySQL |
| `MYSQL_DATABASE` | `campusmart` | MySQL |
| `MYSQL_USER` | `campusmart` | Compose 默认值 |
| `MYSQL_PASSWORD` | `campusmart` | Compose 默认值 |
| `NACOS_SERVER_ADDR` | `nacos:8848` | 业务容器 |
| `NACOS_NAMESPACE` | `campusmart` | 业务容器 |
| `NACOS_GROUP` | `DEFAULT_GROUP` | 业务容器 |
| `SEARCH_ENGINE_URI` | `http://search-engine:5678` | Gateway |
| `MESSAGE_SERVICE_URI` | `http://message-service:8083` | Gateway |
| `SEARCH_ENGINE_URL` | `http://search-engine:5678` | base-service Compose 环境 |
| `MESSAGE_DB_DSN` | `root:root@tcp(mysql:3306)/campusmart?...` | message-service |
| `RABBITMQ_HOST` | `rabbitmq` | base/search/ai |
| `MINIO_ENDPOINT` | `http://minio:9000` | base/message/ai |

> 注意：`SearchEngineClient` 读取的属性名是 `search-engine.url`，当前 Compose 设置的是 `SEARCH_ENGINE_URL`。Spring Boot relaxed binding 通常能映射为 `search-engine.url`，但如果后续改名，需要同时校验 Feign 配置。

---

## 5. 典型业务流程

### 商品搜索

```text
Android
  -> GET /app/goods/search?current=1&size=10&titleKeyword=...
  -> Gateway
  -> base-service
  -> SearchEngineService.searchGoods
  -> Feign POST /api/query?database=campusmart
  -> search-engine
  -> 返回搜索结果
```

如果搜索服务不可用，`GoodsController` 会捕获异常并回退到数据库模糊查询。

### 商品新增 / 修改

```text
Android
  -> Gateway
  -> base-service 保存或更新商品
  -> base-service 构建索引任务
  -> RabbitMQ index_exchange / index.update.base
  -> search-engine 消费任务并写入索引
  -> 如 imageURL 不为空，search-engine 投递 tagging.task
  -> ai-tagging 消费打标任务
  -> ai-tagging 写回 index_exchange / index.update.ai
  -> search-engine 再次消费并更新带 tags 的索引
```

### 商品删除

```text
Android
  -> Gateway
  -> base-service 逻辑删除商品
  -> Feign POST /api/index/remove
  -> search-engine 删除索引
```

### 聊天消息

```text
Android
  -> ws://host:8080/ws?userId=xxx&token=jwt
  -> Gateway WebSocketAuthFilter 校验 JWT
  -> message-service
  -> 写入 MySQL
  -> WebSocket 推送给发送方与接收方当前在线连接
```

---

## 6. 验证命令

```bash
# 启动
cd deploy/docker
docker compose up -d --build

# 查看服务
docker compose ps

# 查看日志
docker compose logs -f gateway
docker compose logs -f base-service
docker compose logs -f message-service
docker compose logs -f search-engine
docker compose logs -f ai-tagging

# 基础设施检查
docker exec -it campusmart-redis redis-cli ping
docker exec -it campusmart-mysql mysql -uroot -proot -e "SHOW DATABASES;"

# Nacos 控制台
open http://localhost:8848/nacos/

# RabbitMQ 控制台
open http://localhost:15672

# MinIO 控制台
open http://localhost:9001
```

---

## 7. 当前风险与待确认项

1. `ai-tagging` 端口配置存在不一致：应用默认 8080，Compose 映射 8084 到容器 8084。
2. `search-engine` 当前未注册到 Nacos，Gateway 和 Feign 都依赖固定 URL 或 Compose 服务名。
3. `ai-tagging` 当前未注册到 Nacos，主要通过 RabbitMQ 参与链路。
4. `deploy/docker/docker-compose.yml` 挂载 `./init-sql:/docker-entrypoint-initdb.d`，但当前 `deploy/docker/init-sql` 未找到；如果 Nacos 需要 MySQL 持久化库 `nacos_config`，需要确认初始化脚本是否补齐。
5. Nacos Config 依赖已引入，但 `gateway` 和 `base-service` 当前配置为 `enabled: false`，实际配置仍以本地 `application.yml` 和环境变量为主。

---

## 8. 维护建议

后续文档建议以本文档为唯一实现口径。若继续推进架构完善，优先处理：

1. 修正 `ai-tagging` 端口映射或应用监听端口。
2. 明确是否要让 `search-engine` 和 `ai-tagging` 注册到 Nacos。
3. 如果需要 Nacos 持久化，补齐 `nacos_config` 初始化 SQL。
4. 统一 `SEARCH_ENGINE_URI`、`SEARCH_ENGINE_URL`、`search-engine.url` 的命名与配置来源。
5. 将旧的实现文档保留为跳转页，避免多份文档描述互相冲突。
