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

	"campusmart/payment-service/internal/config"
	"campusmart/payment-service/internal/handler"
	"campusmart/payment-service/internal/middleware"
	"campusmart/payment-service/internal/model"
	"campusmart/payment-service/internal/mq"
	"campusmart/payment-service/internal/repository"
	"campusmart/payment-service/internal/router"
	"campusmart/payment-service/internal/service"
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

	if err := db.AutoMigrate(&model.AlipayAccountBind{}, &model.Payment{}, &model.PaymentOutbox{}); err != nil {
		logger.Fatalf("migrate db: %v", err)
	}

	bindRepo := repository.NewAlipayBindRepository(db)
	paymentRepo := repository.NewPaymentRepository(db)
	bindSvc := service.NewAlipayBindService(bindRepo)
	paymentSvc := service.NewPaymentService(paymentRepo, bindRepo, service.PaymentConfig{
		AlipayAppID:           cfg.AlipayAppID,
		AlipayNotifyURL:       cfg.AlipayNotifyURL,
		AlipayMockSignSecret:  cfg.AlipayMockSignSecret,
		AlipayVerifySignature: cfg.AlipayVerifySignature,
	})
	bindHandler := handler.NewAlipayBindHandler(bindSvc, cfg.JWTSecret)
	paymentHandler := handler.NewPaymentHandler(paymentSvc, cfg.JWTSecret)

	outboxCtx, stopOutbox := context.WithCancel(context.Background())
	defer stopOutbox()
	outboxDispatcher, err := mq.NewOutboxDispatcher(cfg.RabbitMQURL, paymentRepo, logger)
	if err != nil {
		logger.Printf("payment outbox dispatcher disabled: %v", err)
	} else {
		defer outboxDispatcher.Close()
		go outboxDispatcher.Start(outboxCtx)
	}

	r := gin.New()
	r.Use(gin.Recovery())
	r.Use(middleware.CORS())
	router.Register(r, bindHandler, paymentHandler)

	addr := ":" + cfg.Port
	srv := &http.Server{
		Addr:              addr,
		Handler:           r,
		ReadHeaderTimeout: 10 * time.Second,
	}

	go func() {
		logger.Printf("payment-service listening on %s", addr)
		if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			logger.Printf("server error: %v", err)
		}
	}()

	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
	sig := <-sigCh
	logger.Printf("signal: %s", sig.String())
	stopOutbox()

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
