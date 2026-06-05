# CampusMart 交易、支付与钱包 Go 微服务实现方案

## 1. 方案目标

本方案用于在 CampusMart 中实现类似闲鱼的校园二手交易闭环：

1. 买家对商品下单。
2. 买家通过支付宝沙箱模拟付款。
3. 平台在系统内模拟资金托管。
4. 买卖双方线下完成交易，不设计物流流程。
5. 买家点击确认收货。
6. 系统把托管金额结算到卖家钱包，并展示为卖家绑定的支付宝沙箱账户已模拟到账。

支付宝沙箱只负责模拟“买家支付给平台商户”。确认收货后的卖家到账不调用支付宝真实转账接口，而是由 `wallet-service` 在 CampusMart 内部通过钱包余额和资金流水模拟。

## 2. 服务拆分

本方案将交易链路拆分为三个 Go 微服务：

```text
trade-service     订单、交易状态、下单、取消、确认收货
payment-service   支付宝沙箱绑定、支付单、支付参数生成、支付回调
wallet-service    钱包、托管资金、卖家模拟到账、钱包流水
```

现有 Java `base-service` 继续负责用户、商品、图片等主业务。三个 Go 服务通过 HTTP 和 RabbitMQ 与现有服务协作。

### 2.1 trade-service

职责：

1. 创建订单。
2. 维护订单状态机。
3. 校验买家、卖家、商品关系。
4. 发送支付超时延迟消息。
5. 消费支付成功事件，把订单从 `CREATED` 改为 `PAID`。
6. 消费支付超时消息，把未支付订单改为 `CANCELLED`。
7. 处理买家确认收货，把订单改为 `SETTLED`。
8. 调用或发送事件给 `wallet-service` 完成卖家结算。

### 2.2 payment-service

职责：

1. 管理用户绑定的支付宝沙箱账户。
2. 创建和维护支付单。
3. 生成支付宝沙箱 App 支付 `orderString`。
4. 接收支付宝异步回调并验签。
5. 校验金额、订单号、商户应用。
6. 支付成功后发布 `payment.paid` 事件。
7. 关闭超时未支付的支付单。

### 2.3 wallet-service

职责：

1. 管理用户钱包。
2. 记录平台托管流水。
3. 处理确认收货后的卖家入账。
4. 保存钱包流水。
5. 提供钱包余额、流水查询和模拟提现接口。
6. 返回卖家绑定支付宝沙箱账号，用于 App 展示模拟到账。

## 3. 推荐目录结构

```text
server/trade-service
  cmd/trade-service/main.go
  internal/config
  internal/domain/order
  internal/handler
  internal/repository
  internal/service
  internal/client
  internal/mq
  internal/pkg
  migrations
  go.mod

server/payment-service
  cmd/payment-service/main.go
  internal/config
  internal/domain/payment
  internal/domain/alipaybind
  internal/handler
  internal/repository
  internal/service
  internal/mq
  internal/pkg/alipay
  migrations
  go.mod

server/wallet-service
  cmd/wallet-service/main.go
  internal/config
  internal/domain/wallet
  internal/handler
  internal/repository
  internal/service
  internal/client
  internal/mq
  migrations
  go.mod
```

Go 技术栈建议：

```text
HTTP 框架：Gin
数据库：MySQL
ORM/SQL：GORM 或 sqlc，第一版用 GORM 更快
MQ：RabbitMQ，github.com/rabbitmq/amqp091-go
配置：Viper 或 envconfig
日志：zap 或 slog
金额：decimal.Decimal，避免 float64
支付宝 SDK：github.com/smartwalle/alipay/v3，或直接按支付宝开放平台规范签名请求
```

## 4. 订单状态设计

第一版只保留最小闭环状态：

| 状态 | 含义 | 所属服务 |
| --- | --- | --- |
| `CREATED` | 订单已创建，等待买家支付 | trade-service |
| `PAID` | 买家已支付，资金平台内托管 | trade-service |
| `SETTLED` | 买家已确认收货，卖家已模拟到账 | trade-service |
| `CANCELLED` | 未支付订单被买家取消或超时取消 | trade-service |
| `REFUNDED` | 已退款，第一版可只预留 | trade-service |

状态流转：

```text
CREATED -> PAID -> SETTLED
CREATED -> CANCELLED
PAID -> REFUNDED
```

第一版核心闭环：

```text
CREATED -> PAID -> SETTLED
CREATED -> CANCELLED
```

## 5. 数据库拆分

推荐每个服务拥有自己的库，避免跨服务直接读写表。

```text
campusmart_trade
campusmart_payment
campusmart_wallet
```

如果本地部署想简化，也可以先用同一个 MySQL 实例，不同数据库名区分服务边界。

## 6. trade-service 数据库

### 6.1 订单表 `orders`

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    goods_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    pay_expire_at DATETIME NOT NULL,
    paid_at DATETIME NULL,
    settled_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    cancel_reason VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_buyer_id (buyer_id),
    INDEX idx_seller_id (seller_id),
    INDEX idx_goods_id (goods_id),
    INDEX idx_status_expire (status, pay_expire_at)
);
```

### 6.2 订单事件表 `order_events`

用于记录订单状态变化，方便排查问题。

```sql
CREATE TABLE order_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_body TEXT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_order_no (order_no)
);
```

### 6.3 Outbox 表 `trade_outbox`

用于保证订单状态更新和事件发送最终一致。

```sql
CREATE TABLE trade_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    event_type VARCHAR(64) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_retry (status, next_retry_at)
);
```

## 7. payment-service 数据库

### 7.1 支付单表 `payments`

```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    pay_no VARCHAR(64) NOT NULL UNIQUE,
    buyer_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    channel VARCHAR(32) NOT NULL DEFAULT 'ALIPAY_SANDBOX',
    status VARCHAR(32) NOT NULL,
    alipay_trade_no VARCHAR(128) NULL,
    notify_body TEXT NULL,
    paid_at DATETIME NULL,
    closed_at DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order_id (order_id),
    INDEX idx_order_no (order_no),
    INDEX idx_buyer_id (buyer_id)
);
```

支付状态：

```text
WAIT_PAY
SUCCESS
CLOSED
FAILED
REFUNDED
```

### 7.2 支付宝沙箱绑定表 `alipay_account_binds`

```sql
CREATE TABLE alipay_account_binds (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    alipay_user_id VARCHAR(128) NOT NULL,
    alipay_login_id VARCHAR(128) NULL,
    nickname VARCHAR(64) NULL,
    bind_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
```

说明：

1. 同一个 CampusMart 用户既可以买也可以卖，不需要保存买家/卖家角色。
2. 买东西时，该绑定账户展示为买家支付宝沙箱账号。
3. 卖东西时，该绑定账户展示为卖家支付宝沙箱账号。
4. 卖家模拟到账由 `wallet-service` 完成，不是真的写入支付宝个人沙箱余额。

### 7.3 支付事件 Outbox 表 `payment_outbox`

```sql
CREATE TABLE payment_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    event_type VARCHAR(64) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_retry (status, next_retry_at)
);
```

## 8. wallet-service 数据库

### 8.1 钱包表 `user_wallets`

```sql
CREATE TABLE user_wallets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    available_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    frozen_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0
);
```

### 8.2 钱包流水表 `wallet_flows`

```sql
CREATE TABLE wallet_flows (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    order_no VARCHAR(64) NULL,
    flow_no VARCHAR(64) NOT NULL UNIQUE,
    flow_type VARCHAR(32) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    balance_after DECIMAL(10, 2) NOT NULL,
    remark VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_order_id (order_id),
    UNIQUE KEY uk_order_flow_type (order_id, flow_type)
);
```

流水类型：

```text
ESCROW_IN       平台托管入账
SELLER_INCOME   卖家确认收货到账
WITHDRAW_OUT    用户模拟提现扣款
REFUND_OUT      退款出账，第一版可预留
```

提现不单独建提现单，也不调用支付宝转账接口。用户发起提现后，`wallet-service` 只校验余额、扣减 `user_wallets.available_amount`，并写入 `wallet_flows` 的 `WITHDRAW_OUT` 流水。

### 8.3 钱包事件 Outbox 表 `wallet_outbox`

```sql
CREATE TABLE wallet_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL UNIQUE,
    event_type VARCHAR(64) NOT NULL,
    routing_key VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_retry (status, next_retry_at)
);
```

## 9. 服务接口设计

对 App 暴露的接口可以继续走现有 gateway，按服务转发到 Go 微服务。

### 9.1 trade-service 接口

#### 创建订单

```http
POST /app/orders
```

请求：

```json
{
  "goodsId": 10001
}
```

处理逻辑：

1. 从登录态获取买家 `buyerId`。
2. 调用 `base-service` 查询商品信息和卖家。
3. 校验商品可购买，买家不能购买自己的商品。
4. 创建 `orders`，状态为 `CREATED`。
5. 调用 `payment-service` 创建支付单。
6. 锁定商品，避免重复购买。
7. 发送支付超时延迟消息。

返回：

```json
{
  "orderId": 1,
  "orderNo": "CM202605220001",
  "status": "CREATED",
  "amount": "99.00",
  "payExpireAt": "2026-05-22 03:50:00"
}
```

#### 查询订单

```http
GET /app/orders/{orderId}
GET /app/orders/buyer
GET /app/orders/seller
```

#### 主动取消订单

```http
POST /app/orders/{orderId}/cancel
```

只允许买家取消 `CREATED` 状态订单。取消成功后通知 `payment-service` 关闭支付单，并通知 `base-service` 释放商品锁定。

#### 确认收货

```http
POST /app/orders/{orderId}/confirm-receipt
```

处理逻辑：

1. 校验当前用户是订单买家。
2. 校验订单状态是 `PAID`。
3. 条件更新订单为 `SETTLED`。
4. 发布 `trade.order.settled` 事件。
5. `wallet-service` 消费事件，给卖家钱包入账。
6. 返回卖家模拟到账结果。

返回：

```json
{
  "orderId": 1,
  "status": "SETTLED",
  "settledAmount": "99.00",
  "message": "已确认收货，卖家支付宝沙箱账户已模拟到账"
}
```

### 9.2 payment-service 接口

#### 模拟绑定支付宝沙箱账户

```http
POST /app/alipay/bind/mock
```

请求：

```json
{
  "alipayUserId": "2088000000000001",
  "alipayLoginId": "buyer_sandbox@alipay.com",
  "nickname": "买家沙箱账户"
}
```

#### 查询绑定状态

```http
GET /app/alipay/bind
```

#### 解绑

```http
DELETE /app/alipay/bind
```

#### 创建支付单，服务间接口

```http
POST /internal/payments
```

由 `trade-service` 在创建订单后调用。

请求：

```json
{
  "orderId": 1,
  "orderNo": "CM202605220001",
  "buyerId": 10,
  "amount": "99.00"
}
```

#### 发起支付宝沙箱支付

```http
POST /app/orders/{orderId}/pay/alipay
```

处理逻辑：

1. 校验买家已绑定支付宝沙箱账号。
2. 查询支付单必须是 `WAIT_PAY`。
3. 生成支付宝沙箱 App 支付 `orderString`。
4. 返回给 Android 端拉起支付宝沙箱支付。

返回：

```json
{
  "orderString": "app_id=xxx&biz_content=xxx&sign=xxx"
}
```

#### 支付宝异步回调

```http
POST /app/payments/alipay/notify
```

处理逻辑：

1. 接收支付宝沙箱异步通知。
2. 使用支付宝公钥验签。
3. 校验 `app_id`、`out_trade_no`、`total_amount`。
4. 根据 `out_trade_no` 查询支付单。
5. 幂等判断：如果已是 `SUCCESS`，直接返回 `success`。
6. 如果是 `WAIT_PAY`，更新支付单为 `SUCCESS`。
7. 保存 `alipay_trade_no` 和回调原文。
8. 发布 `payment.paid` 事件。
9. 返回 `success` 给支付宝。

#### 关闭支付单，服务间接口

```http
POST /internal/payments/{orderId}/close
```

由 `trade-service` 在主动取消或超时取消订单后调用。

### 9.3 wallet-service 接口

#### 查询我的钱包

```http
GET /app/wallet
```

返回：

```json
{
  "userId": 20,
  "availableAmount": "99.00",
  "frozenAmount": "0.00"
}
```

#### 查询钱包流水

```http
GET /app/wallet/flows
```

#### 模拟提现

```http
POST /app/wallet/withdraw
```

请求：

```json
{
  "amount": "50.00"
}
```

处理逻辑：

1. 获取当前登录用户。
2. 校验提现金额大于 0，且最多保留两位小数。
3. 查询或初始化用户钱包。
4. 校验 `available_amount >= amount`。
5. 扣减 `user_wallets.available_amount`。
6. 写入 `wallet_flows`，流水类型为 `WITHDRAW_OUT`。
7. 返回扣款后的钱包余额。

返回：

```json
{
  "userId": 20,
  "withdrawAmount": "50.00",
  "availableAmount": "49.00",
  "flowType": "WITHDRAW_OUT",
  "message": "模拟提现成功"
}
```

说明：

1. 第一版不创建提现单。
2. 第一版不调用支付宝提现或转账接口。
3. 这里的提现只是 CampusMart 钱包内的余额扣减和流水记录，用于模拟用户从平台钱包取现。

#### 订单结算，服务间接口

如果第一版想简化，也可以由 `trade-service` 在确认收货时同步调用：

```http
POST /internal/wallet/settle-order
```

请求：

```json
{
  "orderId": 1,
  "orderNo": "CM202605220001",
  "sellerId": 20,
  "amount": "99.00"
}
```

更推荐事件驱动：`wallet-service` 消费 `trade.order.settled` 事件完成入账。

## 10. MQ 事件设计

RabbitMQ 交换机建议：

```text
campusmart.trade.delay.exchange
campusmart.payment.exchange
campusmart.trade.exchange
campusmart.wallet.exchange
```

### 10.1 支付超时延迟消息

创建订单成功后，`trade-service` 发送延迟消息：

```text
exchange: campusmart.trade.delay.exchange
routingKey: trade.order.payment.timeout
delay: 15 minutes
queue: campusmart.trade.order.payment.timeout.queue
```

消息体：

```json
{
  "orderId": 1,
  "orderNo": "CM202605220001"
}
```

消费者仍在 `trade-service` 内部。

消费逻辑：

1. 查询订单。
2. 如果订单不是 `CREATED`，直接 ack。
3. 如果订单是 `CREATED`，条件更新为 `CANCELLED`。
4. 调用 `payment-service` 关闭支付单。
5. 调用 `base-service` 释放商品锁定。

### 10.2 支付成功事件 `payment.paid`

由 `payment-service` 发布，`trade-service` 和 `wallet-service` 消费。

```json
{
  "eventId": "EVT202605220001",
  "orderId": 1,
  "orderNo": "CM202605220001",
  "payNo": "PAY202605220001",
  "buyerId": 10,
  "amount": "99.00",
  "alipayTradeNo": "202605222200000000001"
}
```

`trade-service` 消费后：

```sql
UPDATE orders
SET status = 'PAID', paid_at = NOW()
WHERE id = ? AND status = 'CREATED';
```

`wallet-service` 消费后：

1. 写入 `ESCROW_IN` 托管流水。
2. 不增加卖家可用余额。
3. 通过唯一键保证同一订单只写一次托管流水。

### 10.3 订单结算事件 `trade.order.settled`

由 `trade-service` 在确认收货成功后发布，`wallet-service` 消费。

```json
{
  "eventId": "EVT202605220002",
  "orderId": 1,
  "orderNo": "CM202605220001",
  "buyerId": 10,
  "sellerId": 20,
  "amount": "99.00",
  "settledAt": "2026-05-22 04:10:00"
}
```

`wallet-service` 消费后：

1. 初始化或查询卖家钱包。
2. `available_amount += amount`。
3. 写入 `SELLER_INCOME` 流水。
4. 查询卖家绑定支付宝沙箱账户，用于展示到账账户。
5. 保证幂等：`wallet_flows` 中 `(order_id, flow_type)` 唯一。

## 11. 一致性和幂等

### 11.1 支付回调和超时取消并发

支付宝回调和支付超时消息可能同时到达。必须通过状态条件更新控制最终结果。

支付成功：

```sql
UPDATE orders
SET status = 'PAID', paid_at = NOW()
WHERE id = ? AND status = 'CREATED';
```

超时取消：

```sql
UPDATE orders
SET status = 'CANCELLED', cancelled_at = NOW(), cancel_reason = 'PAY_TIMEOUT'
WHERE id = ? AND status = 'CREATED';
```

谁先更新成功，谁获得状态流转权。另一个流程发现影响行数为 0 后重新查询订单状态，并按幂等逻辑处理。

### 11.2 事件幂等

每个 MQ 消息带 `eventId`，消费者本地也要有业务唯一约束：

```text
payment-service: payments.uk_order_id
wallet-service: wallet_flows.uk_order_flow_type
trade-service: orders.status 条件更新
```

### 11.3 Outbox 保障事件发送

涉及“更新数据库 + 发 MQ”的地方建议使用 Outbox：

1. 在同一个数据库事务中更新业务表和插入 outbox。
2. 后台 goroutine 扫描 `NEW` 事件。
3. 发布 MQ 成功后把 outbox 改为 `SENT`。
4. 发布失败则递增重试次数。

第一版如果时间紧，可以先直接发 MQ，但支付回调和确认收货建议优先使用 Outbox。

## 12. 配置项

### 12.1 trade-service

```yaml
server:
  port: 8091

mysql:
  dsn: ${TRADE_MYSQL_DSN}

rabbitmq:
  url: ${RABBITMQ_URL}

campusmart:
  order:
    pay-timeout-minutes: 15

clients:
  base-service: http://localhost:8081
  payment-service: http://localhost:8092
  wallet-service: http://localhost:8093
```

### 12.2 payment-service

```yaml
server:
  port: 8092

mysql:
  dsn: ${PAYMENT_MYSQL_DSN}

rabbitmq:
  url: ${RABBITMQ_URL}

alipay:
  gateway-url: https://openapi-sandbox.dl.alipaydev.com/gateway.do
  app-id: ${ALIPAY_APP_ID}
  merchant-private-key: ${ALIPAY_MERCHANT_PRIVATE_KEY}
  alipay-public-key: ${ALIPAY_PUBLIC_KEY}
  notify-url: ${ALIPAY_NOTIFY_URL}
  return-url: ${ALIPAY_RETURN_URL}
  sign-type: RSA2
```

### 12.3 wallet-service

```yaml
server:
  port: 8093

mysql:
  dsn: ${WALLET_MYSQL_DSN}

rabbitmq:
  url: ${RABBITMQ_URL}

clients:
  payment-service: http://localhost:8092
```

## 13. 实现流程

### 第一步：创建三个 Go 服务骨架

1. 创建 `server/trade-service`、`server/payment-service`、`server/wallet-service`。
2. 每个服务独立 `go.mod`。
3. 接入 Gin、MySQL、RabbitMQ、日志和配置。
4. 每个服务提供 `/health` 健康检查。

### 第二步：实现数据库迁移

1. `trade-service` 创建 `orders`、`order_events`、`trade_outbox`。
2. `payment-service` 创建 `payments`、`alipay_account_binds`、`payment_outbox`。
3. `wallet-service` 创建 `user_wallets`、`wallet_flows`、`wallet_outbox`。
4. 本地使用三个数据库：`campusmart_trade`、`campusmart_payment`、`campusmart_wallet`。

### 第三步：实现 trade-service 订单能力

1. 创建订单接口。
2. 查询买家订单、卖家订单、订单详情。
3. 主动取消订单。
4. 对订单状态更新使用条件更新。
5. 创建订单后调用 `payment-service` 创建支付单。
6. 创建订单后发送支付超时延迟消息。

### 第四步：实现 payment-service 支付宝沙箱绑定

1. 实现 `/app/alipay/bind/mock`。
2. 实现查询绑定和解绑。
3. 支付前校验买家已绑定支付宝沙箱账户。
4. 给 `wallet-service` 提供查询用户绑定支付宝账号的内部接口，便于展示卖家模拟到账账户。

### 第五步：实现 payment-service 支付链路

1. 实现 `/internal/payments` 创建支付单。
2. 接入支付宝沙箱配置。
3. 实现 `/app/orders/{orderId}/pay/alipay` 返回 `orderString`。
4. Android 端用支付宝 SDK 拉起沙箱支付。
5. 实现 `/app/payments/alipay/notify` 验签和支付成功处理。
6. 支付成功后发布 `payment.paid` 事件。

### 第六步：实现 trade-service 支付结果消费

1. 消费 `payment.paid`。
2. 条件更新订单 `CREATED -> PAID`。
3. 如果订单已取消，则按幂等逻辑忽略或进入人工排查。
4. 记录订单事件。

### 第七步：实现 MQ 支付超时取消

1. 声明延迟交换机、队列和 routing key。
2. 创建订单后发送 15 分钟延迟消息。
3. 消费超时消息，取消仍为 `CREATED` 的订单。
4. 关闭支付单并释放商品锁定。

### 第八步：实现 wallet-service 托管和结算

1. 消费 `payment.paid`，写入 `ESCROW_IN` 流水。
2. 消费 `trade.order.settled`，增加卖家钱包余额。
3. 写入 `SELLER_INCOME` 流水。
4. 提供钱包余额和流水查询接口。
5. 提供模拟提现接口，扣减用户可用余额并写入 `WITHDRAW_OUT` 流水。
6. 返回卖家绑定支付宝沙箱账号，供前端展示“已模拟到账”。

### 第九步：实现确认收货

1. `trade-service` 实现确认收货接口。
2. 校验订单必须是 `PAID`。
3. 条件更新为 `SETTLED`。
4. 发布 `trade.order.settled`。
5. `wallet-service` 完成卖家入账。

### 第十步：Android 和网关联调

1. gateway 增加三个 Go 服务路由。
2. 商品详情页增加“立即购买”。
3. 订单详情页展示支付倒计时。
4. 待支付订单展示“去支付”和“取消订单”。
5. 已支付订单展示“确认收货”。
6. 卖家钱包页展示余额和收款流水。
7. 钱包页提供“提现”入口，用户输入金额后调用模拟提现接口。

## 14. 核心业务时序

### 14.1 正常交易流程

```text
买家下单
  -> trade-service 创建订单 CREATED
  -> trade-service 调用 payment-service 创建支付单 WAIT_PAY
  -> trade-service 发送支付超时消息
  -> 买家支付宝沙箱付款
  -> payment-service 接收支付宝回调
  -> payment-service 发布 payment.paid
  -> trade-service 消费 payment.paid，订单变为 PAID
  -> wallet-service 消费 payment.paid，记录 ESCROW_IN
  -> 买卖双方线下交易
  -> 买家确认收货
  -> trade-service 订单变为 SETTLED
  -> trade-service 发布 trade.order.settled
  -> wallet-service 增加卖家钱包余额
  -> App 展示卖家支付宝沙箱账户已模拟到账
```

### 14.2 超时未支付流程

```text
买家下单
  -> 订单 CREATED
  -> 发送 15 分钟支付超时消息
  -> 15 分钟内未支付
  -> trade-service 消费超时消息
  -> 条件更新订单为 CANCELLED
  -> payment-service 关闭支付单
  -> base-service 释放商品锁定
```

### 14.3 确认收货模拟到账流程

```text
买家点击确认收货
  -> trade-service 校验订单为 PAID
  -> trade-service 更新订单为 SETTLED
  -> wallet-service 消费结算事件
  -> user_wallets.available_amount 增加
  -> wallet_flows 写入 SELLER_INCOME
  -> 查询卖家绑定的支付宝沙箱账号
  -> App 展示 seller_sandbox@alipay.com 已到账
```

### 14.4 钱包模拟提现流程

```text
用户进入钱包页
  -> 输入提现金额
  -> wallet-service 校验余额是否足够
  -> user_wallets.available_amount 扣减
  -> wallet_flows 写入 WITHDRAW_OUT
  -> App 展示模拟提现成功和最新余额
```

## 15. 风险和注意事项

1. 支付宝异步回调必须验签，不能只相信请求参数。
2. Go 里金额不要使用 `float64`，使用 decimal 类型或以分为单位的整数。
3. 跨服务不要直接读写对方数据库。
4. 订单状态更新必须带旧状态条件，避免并发错乱。
5. 支付回调必须幂等，同一笔支付宝交易可能多次通知。
6. MQ 消费失败要允许重试，但消费逻辑必须幂等。
7. 卖家“支付宝沙箱账户到账”是 CampusMart 内部模拟，不是真实支付宝账户余额变化。
8. 钱包提现也是 CampusMart 内部模拟，不是真实支付宝提现或银行卡出款。
9. 第一版可以不做真实退款，保留 `REFUNDED` 状态和退款流水类型即可。

## 16. 第一版验收标准

1. 三个 Go 服务可以独立启动，并通过 gateway 访问。
2. 用户可以绑定支付宝沙箱账号。
3. 买家可以创建订单。
4. 创建订单后自动创建支付单。
5. 未支付订单可以主动取消。
6. 未支付订单超过 15 分钟可以被 MQ 自动取消。
7. 买家可以发起支付宝沙箱支付。
8. 支付宝回调后 `payment-service` 支付单变为 `SUCCESS`。
9. `trade-service` 订单变为 `PAID`。
10. 买家确认收货后订单变为 `SETTLED`。
11. `wallet-service` 卖家钱包余额增加。
12. 卖家流水中出现一条 `SELLER_INCOME` 收款记录。
13. 用户可以从钱包发起模拟提现。
14. 模拟提现后钱包余额扣减，流水中出现一条 `WITHDRAW_OUT` 记录。
15. App 可以展示“卖家支付宝沙箱账户已模拟到账”。
