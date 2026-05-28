# payment-service

`payment-service` manages payment-related capabilities for CampusMart. The current implementation includes the Alipay sandbox account binding APIs.

## Environment

```bash
export PAYMENT_DB_DSN='root:root@tcp(localhost:3306)/campusmart?charset=utf8mb4&parseTime=True&loc=Local'
export RABBITMQ_URL='amqp://guest:guest@localhost:5672/'
export JWT_SECRET='cX6NdmHd6tk3pyexe5tWjvKcZtnPxztv'
export PORT=8092
export ALIPAY_APP_ID='campusmart-sandbox-app'
export ALIPAY_NOTIFY_URL='http://localhost:8080/app/payments/alipay/notify'
export ALIPAY_MOCK_SIGN_SECRET='campusmart-payment-mock-secret'
export ALIPAY_VERIFY_SIGNATURE=false
```

## APIs

Use the same `access-token` header returned by the existing Java login API. For local debugging, `X-User-Id` is also supported.

```http
POST /app/alipay/bind/mock
GET /app/alipay/bind
DELETE /app/alipay/bind
GET /internal/alipay/binds/{userId}
POST /internal/payments
POST /internal/payments/{orderId}/close
POST /app/orders/{orderId}/pay/alipay
POST /app/payments/alipay/notify
```

Example request:

```json
{
  "alipayUserId": "2088000000000001",
  "alipayLoginId": "buyer_sandbox@alipay.com",
  "nickname": "买家沙箱账户"
}
```

Create payment request:

```json
{
  "orderId": 1,
  "orderNo": "CM202605220001",
  "buyerId": 10,
  "amount": "99.00"
}
```
