package config

import "os"

type Config struct {
	Port        string
	MySQLDSN    string
	RabbitMQURL string
	JWTSecret   string
}

func Load() Config {
	return Config{
		Port:        envOrDefault("PORT", "8093"),
		MySQLDSN:    envOrDefault("WALLET_DB_DSN", "root:root@tcp(localhost:3306)/campusmart?charset=utf8mb4&parseTime=True&loc=Local"),
		RabbitMQURL: envOrDefault("RABBITMQ_URL", "amqp://guest:guest@localhost:5672/"),
		JWTSecret:   envOrDefault("JWT_SECRET", "cX6NdmHd6tk3pyexe5tWjvKcZtnPxztv"),
	}
}

func envOrDefault(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
