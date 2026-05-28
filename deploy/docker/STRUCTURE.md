# 合并后的项目结构

## 目录结构

```
CampusMart/
├── Android/                    # Android 客户端
├── server/                     # 后端服务
│   ├── gateway/                # API 网关 (Spring Cloud Gateway)
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/
│   │       └── main/
│   │           ├── java/
│   │           │   └── org/example/campusmart/gateway/
│   │           └── resources/
│   │               └── application.yml
│   ├── base-service/           # 基础服务 (Java Spring Boot)
│   │   ├── common/
│   │   ├── model/
│   │   └── web/
│   │       ├── Dockerfile
│   │       ├── pom.xml
│   │       └── src/
│   ├── message-service-go/     # 消息服务 (Go)
│   │   ├── Dockerfile          # (新增)
│   │   ├── go.mod
│   │   ├── go.sum
│   │   ├── main.go
│   │   └── ...
│   ├── payment-service/        # 支付服务 (Go)
│   │   ├── Dockerfile
│   │   ├── go.mod
│   │   ├── go.sum
│   │   ├── cmd/
│   │   └── internal/
│   ├── trade-service/          # 订单交易服务 (Go)
│   │   ├── Dockerfile
│   │   ├── go.mod
│   │   ├── go.sum
│   │   ├── cmd/
│   │   └── internal/
│   ├── wallet-service/         # 钱包服务 (Go)
│   │   ├── Dockerfile
│   │   ├── go.mod
│   │   ├── go.sum
│   │   ├── cmd/
│   │   └── internal/
│   ├── search-engine/          # 搜索引擎 (从 MixFound 复制)
│   │   ├── Dockerfile
│   │   ├── config.yaml
│   │   ├── go.mod
│   │   └── ...
│   └── ai-tagging/             # AI 打标服务 (从 MixFound 复制)
│       ├── Dockerfile
│       ├── requirements.txt
│       └── ...
├── deploy/                     # 部署配置
│   └── docker/
│       ├── docker-compose.yml  # 统一部署配置
│       ├── .env.example        # 环境变量示例
│       ├── init.sh             # 初始化脚本
│       └── STRUCTURE.md        # 本文件
├── IMPLEMENTATION_DOC.md
├── NACOS_IMPLEMENTATION.md
└── SERVICE_COMMUNICATION.md
```

## 初始化步骤

### 第一次使用

```bash
# 1. 进入部署目录
cd CampusMart/deploy/docker

# 2. 复制 .env 文件
cp .env.example .env

# 3. 启动所有服务
docker-compose up -d
```

**注意**: `search-engine` 和 `ai-tagging` 已复制到 `server/` 目录下。

### 启动服务

```bash
cd CampusMart/deploy/docker

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 停止服务

```bash
cd CampusMart/deploy/docker
docker-compose down
```

## 服务说明

| 服务 | 语言 | 容器端口 | 宿主机端口 | 说明 |
|------|------|----------|------------|------|
| gateway | Java | 8080 | 8080 | API 网关，统一鉴权、路由 |
| base-service | Java | 8081 | - | 基础业务服务 |
| message-service | Go | 8083 | - | 消息服务 (WebSocket) |
| payment-service | Go | 8092 | - | 支付宝沙箱绑定和支付服务 |
| trade-service | Go | 8091 | - | 订单交易和状态流转服务 |
| wallet-service | Go | 8093 | - | 钱包、流水和模拟提现服务 |
| search-engine | Go | 5678 | - | 搜索引擎 |
| ai-tagging | Python | 8080 | - | AI 图片打标 |
| mysql | - | 3306 | 3306 | 数据库 |
| redis | - | 6379 | 6379 | 缓存 |
| rabbitmq | - | 5672/15672 | 5672/15672 | 消息队列 |
| nacos | - | 8848 | 8848 | 注册/配置中心 |
| minio | - | 9000/9001 | 9000/9001 | 对象存储 |

## 服务依赖关系

```
gateway
  │
  ├── base-service
  │     ├── mysql
  │     ├── redis
  │     ├── rabbitmq
  │     ├── nacos
  │     ├── minio
  │     └── search-engine (通过 HTTP)
  │
  ├── message-service
  │     ├── mysql
  │     ├── nacos
  │     └── minio
  │
  ├── payment-service
  │     ├── mysql
  │     └── rabbitmq
  │
  ├── trade-service
  │     ├── mysql
  │     ├── rabbitmq
  │     └── payment-service
  │
  ├── wallet-service
  │     ├── mysql
  │     └── rabbitmq
  │
  └── (Android 客户端通过 WebSocket)
        └── message-service
```

## 访问地址

| 服务 | 地址 |
|------|------|
| Gateway (API入口) | http://localhost:8080 |
| Nacos 控制台 | http://localhost:8848/nacos (nacos/nacos) |
| RabbitMQ 管理界面 | http://localhost:15672 (guest/guest) |
| MinIO 控制台 | http://localhost:9001 (minioadmin/minioadmin) |

## 开发模式

如果需要单独启动某个服务进行调试：

```bash
# 只启动基础设施
docker-compose up -d mysql redis rabbitmq nacos minio

# 手动启动 Java 服务（需要先编译）
cd ../../server/base-service/web
mvn clean package
java -jar target/*.jar

# 手动启动 Go 服务
cd ../../server/message-service-go
go run main.go
```

## Dockerfile 列表

| 服务 | Dockerfile 路径 |
|------|-----------------|
| gateway | server/gateway/Dockerfile |
| base-service | server/base-service/web/Dockerfile |
| message-service | server/message-service-go/Dockerfile |
| payment-service | server/payment-service/Dockerfile |
| trade-service | server/trade-service/Dockerfile |
| wallet-service | server/wallet-service/Dockerfile |
| search-engine | server/search-engine/Dockerfile |
| ai-tagging | server/ai-tagging/Dockerfile |
