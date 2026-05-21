package main

import (
	"campusmart/search-engine/internal/core"
	"campusmart/search-engine/internal/global"
	"campusmart/search-engine/internal/queue"
	"campusmart/search-engine/internal/queue/consumer"
	"campusmart/search-engine/internal/queue/producer"
	"campusmart/search-engine/internal/redis"
	"campusmart/search-engine/internal/web/controller"
	"campusmart/search-engine/internal/web/router"
	"context"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"
)

func main() {
	// 全局 panic 恢复
	defer func() {
		if r := recover(); r != nil {
			log.Fatalf("Fatal panic: %v", r)
		}
	}()

	// 1. 初始化核心组件（配置、容器、分词器）
	core.Initialize()

	// 2. 初始化 Redis（提前，因为可能被依赖）
	redis.InitRedisClient()

	// 3. 初始化消息队列
	mqClient, err := queue.NewRabbitMQClient()
	if err != nil {
		log.Fatalf("Failed to connect to RabbitMQ: %v", err)
	}
	defer mqClient.Close()

	// 4. 初始化生产者
	taggingProducer := producer.NewTaggingProducer(mqClient.Channel)

	// 5. 初始化 Services（注入生产者）
	controller.NewServices(taggingProducer)

	// 6. 初始化消费者
	indexConsumer := consumer.NewIndexConsumer(
		mqClient.Channel,
		controller.GetServices().Index,
		[]string{"ai", "base"},
	)

	// 7. 启动消费者
	indexConsumer.Consume()

	// 8. 注册路由
	r := router.SetupRouter()

	// 9. 启动 HTTP 服务
	srv := &http.Server{
		Addr:    global.CONFIG.Addr,
		Handler: r,
	}

	go func() {
		if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			log.Fatalf("listen: %s\n", err)
		}
	}()

	log.Printf("Server started on %s", global.CONFIG.Addr)

	// 10. 优雅关机
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Println("Shutting down server...")

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		log.Println("Server shutdown:", err)
	}

	log.Println("Server exiting")
}
