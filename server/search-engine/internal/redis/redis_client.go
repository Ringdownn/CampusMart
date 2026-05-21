package redis

import (
	"context"
	"log"
	"os"
	"sync"

	"github.com/redis/go-redis/v9"
	"gopkg.in/yaml.v2"
)

var (
	Rdb  *redis.Client
	once sync.Once
	Ctx  = context.Background()
)

type RedisConfig struct {
	RedisAddr string `yaml:"redis-addr"`
	RedisPass string `yaml:"redis-pass"`
	RedisDB   int    `yaml:"redis-DB"`
}

func loadRedisConfig() *RedisConfig {
	configFile := "config.yaml"

	// 默认配置
	defaultConfig := &RedisConfig{
		RedisAddr: "127.0.0.1:6379",
		RedisPass: "",
		RedisDB:   0,
	}

	// 读取配置文件
	file, err := os.ReadFile(configFile)
	if err != nil {
		return defaultConfig
	}

	// 解析 YAML
	var config map[string]interface{}
	err = yaml.Unmarshal(file, &config)
	if err != nil {
		return defaultConfig
	}

	// 提取 Redis 配置（嵌套结构）
	redisConfig := defaultConfig
	if redisSection, ok := config["redis"].(map[interface{}]interface{}); ok {
		if redisAddr, ok := redisSection["addr"].(string); ok && redisAddr != "" {
			redisConfig.RedisAddr = redisAddr
		}
		if redisPass, ok := redisSection["password"].(string); ok {
			redisConfig.RedisPass = redisPass
		}
		if redisDB, ok := redisSection["DB"].(float64); ok {
			redisConfig.RedisDB = int(redisDB)
		}
	}

	return redisConfig
}

func InitRedisClient() {
	once.Do(func() {
		redisConfig := loadRedisConfig()

		Rdb = redis.NewClient(&redis.Options{
			Addr:     redisConfig.RedisAddr,
			Password: redisConfig.RedisPass,
			DB:       redisConfig.RedisDB,
		})

		// 测试连接
		if err := Rdb.Ping(Ctx).Err(); err != nil {
			log.Fatalf("Failed to connect to Redis: %v", err)
		}

		log.Println("Redis connected successfully")
	})
}

func Close() {
	if Rdb != nil {
		if err := Rdb.Close(); err != nil {
			log.Printf("Error closing Redis: %v", err)
		}
	}
}
