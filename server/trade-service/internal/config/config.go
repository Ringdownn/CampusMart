package config

import (
	"os"
	"strconv"
)

type Config struct {
	Port              string
	MySQLDSN          string
	RabbitMQURL       string
	JWTSecret         string
	PaymentServiceURL string
	PayTimeoutMinutes int
}

func Load() Config {
	return Config{
		Port:              envOrDefault("PORT", "8091"),
		MySQLDSN:          envOrDefault("TRADE_DB_DSN", "root:root@tcp(localhost:3306)/campusmart?charset=utf8mb4&parseTime=True&loc=Local"),
		RabbitMQURL:       envOrDefault("RABBITMQ_URL", "amqp://guest:guest@localhost:5672/"),
		JWTSecret:         envOrDefault("JWT_SECRET", "cX6NdmHd6tk3pyexe5tWjvKcZtnPxztv"),
		PaymentServiceURL: envOrDefault("PAYMENT_SERVICE_URL", "http://localhost:8092"),
		PayTimeoutMinutes: envIntOrDefault("PAY_TIMEOUT_MINUTES", 15),
	}
}

func envOrDefault(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}

func envIntOrDefault(key string, fallback int) int {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	parsed, err := strconv.Atoi(value)
	if err != nil || parsed <= 0 {
		return fallback
	}
	return parsed
}
