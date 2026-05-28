package config

import "os"

type Config struct {
	Port                  string
	MySQLDSN              string
	RabbitMQURL           string
	JWTSecret             string
	AlipayAppID           string
	AlipayNotifyURL       string
	AlipayMockSignSecret  string
	AlipayVerifySignature bool
}

func Load() Config {
	return Config{
		Port:                  envOrDefault("PORT", "8092"),
		MySQLDSN:              envOrDefault("PAYMENT_DB_DSN", "root:root@tcp(localhost:3306)/campusmart?charset=utf8mb4&parseTime=True&loc=Local"),
		RabbitMQURL:           envOrDefault("RABBITMQ_URL", "amqp://guest:guest@localhost:5672/"),
		JWTSecret:             envOrDefault("JWT_SECRET", "cX6NdmHd6tk3pyexe5tWjvKcZtnPxztv"),
		AlipayAppID:           envOrDefault("ALIPAY_APP_ID", "campusmart-sandbox-app"),
		AlipayNotifyURL:       envOrDefault("ALIPAY_NOTIFY_URL", "http://localhost:8080/app/payments/alipay/notify"),
		AlipayMockSignSecret:  envOrDefault("ALIPAY_MOCK_SIGN_SECRET", "campusmart-payment-mock-secret"),
		AlipayVerifySignature: envOrDefault("ALIPAY_VERIFY_SIGNATURE", "false") == "true",
	}
}

func envOrDefault(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
