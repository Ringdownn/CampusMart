# 订单服务实现文档（事件驱动 + 消息队列超时取消）

## 1. 目标与范围

本订单服务面向“下单→支付→完成”的交易链路，订单主状态包含 3 个业务状态：

- 待支付（PENDING_PAYMENT）
- 已支付（PAID）
- 已完成（COMPLETED）

同时需要支持：订单在“待支付”状态超过指定时间（如 30 分钟）未支付，自动取消。

说明：自动取消本质上是一个终态，工程上强烈建议额外引入一个系统终态 `CANCELED`（或 `CLOSED`），否则无法区分“仍可支付的待支付”和“已失效的待支付”。下文默认包含 `CANCELED` 这一终态用于实现超时取消。

## 2. 总体架构

采用事件驱动方案，订单服务作为“状态源”（Source of Truth），通过事件通知其他业务（库存、通知、物流等）。核心组件：

- API 层：下单、支付确认、确认完成、查询等
- DB：MySQL（或 Postgres），保存订单与订单明细
- 消息队列：用于业务事件发布、超时取消（延迟消息/死信队列/延迟主题）
- Outbox（推荐）：保证“写库 + 发事件”一致性

核心原则：

- 订单状态迁移由订单服务单点负责
- 所有状态更新必须幂等、可重试
- 事件发布至少一次（at-least-once），消费者侧幂等

## 3. 数据模型（建议）

### 3.1 orders

- id（PK，雪花/UUID/自增均可）
- user_id
- status：PENDING_PAYMENT / PAID / COMPLETED / CANCELED
- total_amount
- currency（可选）
- created_at
- paid_at（可空）
- completed_at（可空）
- canceled_at（可空）
- cancel_reason（可选：TIMEOUT / USER / SYSTEM）
- version（可选：乐观锁）

关键索引：

- (user_id, created_at)
- (status, created_at)

### 3.2 order_items

- id（PK）
- order_id（FK/逻辑外键）
- goods_id
- quantity
- price

### 3.3 outbox_events（推荐）

用于 Outbox Pattern（同一事务写订单与事件，异步转发到 MQ）：

- id（event_id，PK）
- aggregate_type（Order）
- aggregate_id（order_id）
- event_type（OrderCreated/OrderPaid/OrderCanceled/OrderCompleted）
- payload（JSON）
- status（NEW/SENT/FAILED）
- created_at、sent_at

## 4. 状态机与幂等策略

### 4.1 状态机

允许的迁移：

- PENDING_PAYMENT → PAID
- PAID → COMPLETED
- PENDING_PAYMENT → CANCELED（超时取消/用户取消）

不允许的迁移：

- PAID → CANCELED（除非引入退款/逆向流程，此文档不覆盖）

### 4.2 幂等（必须）

所有状态更新使用“条件更新”（CAS）确保并发安全与重复请求可重入：

- 支付回调/支付确认：
  - `UPDATE orders SET status='PAID', paid_at=? WHERE id=? AND status='PENDING_PAYMENT'`
- 完成：
  - `UPDATE orders SET status='COMPLETED', completed_at=? WHERE id=? AND status='PAID'`
- 取消：
  - `UPDATE orders SET status='CANCELED', canceled_at=?, cancel_reason='TIMEOUT' WHERE id=? AND status='PENDING_PAYMENT'`

更新影响行数为 0 时：

- 说明该订单已在目标终态或状态不匹配（重复请求/并发），直接返回成功（幂等语义）

## 5. API 设计（示例）

### 5.1 下单

- POST /orders
- 入参：items、user_id、金额等
- 行为：
  1) DB 事务：创建 orders（PENDING_PAYMENT）+ order_items
  2) DB 事务内写 outbox：OrderCreated（含超时点 expire_at）
  3) 返回 order_id

### 5.2 支付确认（或支付回调落库）

- POST /orders/{id}/pay
- 行为：
  1) 条件更新 PENDING_PAYMENT→PAID
  2) 成功更新则写 outbox：OrderPaid

### 5.3 完成

- POST /orders/{id}/complete
- 行为：
  1) 条件更新 PAID→COMPLETED
  2) 成功更新则写 outbox：OrderCompleted

### 5.4 查询

- GET /orders/{id}
- GET /orders?userId=...&status=...（可选）

## 6. 事件设计（业务事件）

建议事件版本化与固定 schema，便于演进。

### 6.1 事件通用字段

- event_id（全局唯一）
- event_type
- aggregate_id（order_id）
- occurred_at
- payload_version
- payload（业务字段）

### 6.2 事件列表

- OrderCreated
  - order_id, user_id, total_amount, items, created_at, expire_at
- OrderPaid
  - order_id, paid_at, payment_txn_id（可选）
- OrderCanceled
  - order_id, canceled_at, reason=TIMEOUT
- OrderCompleted
  - order_id, completed_at

## 7. 消息队列方案：超时未支付自动取消

超时取消有两种常见做法，推荐“延迟消息/死信”优于“定时扫描”。

### 7.1 RabbitMQ：TTL + DLX（死信交换机）方案（常见）

流程：

1) 下单成功后发送一条“延迟取消消息”到延迟队列 Q_delay_cancel
   - 消息体：order_id
   - TTL：30 分钟（或将过期时间写入消息 header）
2) TTL 到期后消息进入死信交换机（DLX），路由到实际消费队列 Q_cancel
3) 订单服务消费 Q_cancel：
   - 尝试执行 PENDING_PAYMENT→CANCELED 条件更新
   - 成功则写 outbox：OrderCanceled
   - 失败（已支付/已取消）则直接 ack（幂等）

队列建议：

- exchange.order（topic）
- queue.order.cancel.delay（绑定到 delay exchange，带 TTL/DLX）
- queue.order.cancel（实际消费）
- queue.order.cancel.dlq（失败重试/死信，可选）

### 7.2 RocketMQ：延迟消息方案（更直接）

下单后发送一条延迟消息（delay level / deliver time），到期投递给消费者执行取消逻辑。消费者同样用条件更新实现幂等。

### 7.3 Kafka：延迟一般通过“延迟主题 + 调度器”实现

Kafka 不提供原生延迟队列时，常见做法：

- 写入 `order_cancel_delay` topic，包含 expire_at
- 独立调度器（或订单服务内部定时器）按时间轮将到期消息转发到 `order_cancel` topic
- 订单服务消费 `order_cancel` 执行取消

## 8. Outbox Pattern：保证发事件与写库一致

推荐实现：

1) 业务事务内：更新订单 + 插入 outbox_events（NEW）
2) Outbox Relay（后台 goroutine / cron job）轮询 NEW 事件并发布到 MQ
3) 发布成功：标记 SENT（或写 sent_at）
4) 发布失败：标记 FAILED 并按退避重试

这样可以避免：

- 订单状态已更新但消息没发出去
- 消息发出去了但订单状态没更新

## 9. 消费者幂等与重试

事件驱动默认“至少一次投递”，因此消费者必须幂等：

- 用 event_id 去重（落表 processed_events / Redis set 均可）
- 或业务幂等：库存扣减使用“扣减记录表（order_id 唯一）”保证重复消费不重复扣

重试策略：

- MQ 重试（重投）+ 业务幂等
- 超出重试次数进入 DLQ，人工处理/补偿

## 10. 观测与运维建议

- 指标：下单量、支付转化率、取消率、超时取消延迟、MQ 积压、消费失败率
- 日志：order_id 贯穿（trace_id）
- 告警：取消队列积压、Outbox FAILED 增长、支付回调失败

## 11. 最小落地清单（按实现顺序）

- 订单表 + 明细表 +（可选）outbox 表
- 状态机（条件更新 + 幂等返回）
- 下单：写订单 + outbox + 发送“取消延迟消息”
- 支付确认：改 PAID + outbox
- 完成：改 COMPLETED + outbox
- 超时取消消费者：消费到期消息，尝试改 CANCELED + outbox
- Outbox Relay：把 outbox 事件发布到 MQ

