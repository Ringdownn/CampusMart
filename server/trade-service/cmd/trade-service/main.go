package main

import (
	"context"
	"database/sql"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"campusmart/trade-service/internal/client"
	"campusmart/trade-service/internal/config"
	"campusmart/trade-service/internal/handler"
	"campusmart/trade-service/internal/middleware"
	"campusmart/trade-service/internal/model"
	"campusmart/trade-service/internal/mq"
	"campusmart/trade-service/internal/repository"
	"campusmart/trade-service/internal/router"
	"campusmart/trade-service/internal/service"
	"github.com/gin-gonic/gin"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
)

func main() {
	cfg := config.Load()
	logger := log.New(os.Stdout, "", log.LstdFlags)

	db, sqlDB, err := openDB(cfg.MySQLDSN)
	if err != nil {
		logger.Fatalf("open db: %v", err)
	}
	defer sqlDB.Close()

	if err := db.AutoMigrate(&model.Order{}, &model.OrderEvent{}, &model.TradeOutbox{}); err != nil {
		logger.Fatalf("migrate db: %v", err)
	}

	orderRepo := repository.NewOrderRepository(db)
	rabbitClient, err := mq.NewClient(cfg.RabbitMQURL, logger)
	if err != nil {
		logger.Printf("rabbitmq disabled: %v", err)
	}
	if rabbitClient != nil {
		defer rabbitClient.Close()
	}

	paymentClient := client.NewPaymentClient(cfg.PaymentServiceURL)
	orderSvc := service.NewOrderService(orderRepo, paymentClient, cfg.PayTimeoutMinutes, rabbitClient)

	workerCtx, stopWorkers := context.WithCancel(context.Background())
	defer stopWorkers()
	if rabbitClient != nil {
		if err := rabbitClient.StartConsumers(workerCtx, orderSvc); err != nil {
			logger.Printf("start trade consumers failed: %v", err)
		}
		go mq.NewOutboxDispatcher(orderRepo, rabbitClient, logger).Start(workerCtx)
	}

	orderHandler := handler.NewOrderHandler(orderSvc, cfg.JWTSecret)
	r := gin.New()
	r.Use(gin.Recovery())
	r.Use(middleware.CORS())
	router.Register(r, orderHandler)

	addr := ":" + cfg.Port
	srv := &http.Server{
		Addr:              addr,
		Handler:           r,
		ReadHeaderTimeout: 10 * time.Second,
	}
	go func() {
		logger.Printf("trade-service listening on %s", addr)
		if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			logger.Printf("server error: %v", err)
		}
	}()

	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
	sig := <-sigCh
	logger.Printf("signal: %s", sig.String())
	stopWorkers()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(ctx)
}

func openDB(dsn string) (*gorm.DB, *sql.DB, error) {
	db, err := gorm.Open(mysql.Open(dsn), &gorm.Config{})
	if err != nil {
		return nil, nil, err
	}
	sqlDB, err := db.DB()
	if err != nil {
		return nil, nil, err
	}
	sqlDB.SetMaxOpenConns(10)
	sqlDB.SetMaxIdleConns(10)
	sqlDB.SetConnMaxLifetime(5 * time.Minute)

	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()
	if err := sqlDB.PingContext(ctx); err != nil {
		_ = sqlDB.Close()
		return nil, nil, err
	}
	return db, sqlDB, nil
}
