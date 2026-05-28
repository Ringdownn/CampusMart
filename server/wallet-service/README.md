# wallet-service

`wallet-service` manages CampusMart wallet balances, wallet flows, seller settlement, and simulated withdrawals.

## Environment

```bash
export WALLET_DB_DSN='root:root@tcp(localhost:3306)/campusmart?charset=utf8mb4&parseTime=True&loc=Local'
export RABBITMQ_URL='amqp://guest:guest@localhost:5672/'
export JWT_SECRET='cX6NdmHd6tk3pyexe5tWjvKcZtnPxztv'
export PORT=8093
```

## APIs

```http
GET /app/wallet
GET /app/wallet/flows
POST /app/wallet/withdraw
```

Withdraw request:

```json
{
  "amount": "50.00"
}
```
