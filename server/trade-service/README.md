# trade-service

`trade-service` owns CampusMart order state transitions.

## Environment

```bash
export TRADE_DB_DSN='root:root@tcp(localhost:3306)/campusmart?charset=utf8mb4&parseTime=True&loc=Local'
export RABBITMQ_URL='amqp://guest:guest@localhost:5672/'
export PAYMENT_SERVICE_URL='http://localhost:8092'
export JWT_SECRET='cX6NdmHd6tk3pyexe5tWjvKcZtnPxztv'
export PAY_TIMEOUT_MINUTES=15
export PORT=8091
```

## APIs

```http
POST /app/orders
GET /app/orders/{orderId}
GET /app/orders/buyer
GET /app/orders/seller
POST /app/orders/{orderId}/cancel
POST /app/orders/{orderId}/confirm-receipt
```
