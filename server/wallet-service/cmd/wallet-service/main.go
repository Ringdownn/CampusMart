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

	"campusmart/wallet-service/internal/config"
	"campusmart/wallet-service/internal/handler"
	"campusmart/wallet-service/internal/middleware"
	"campusmart/wallet-service/internal/model"
	"campusmart/wallet-service/internal/mq"
	"campusmart/wallet-service/internal/repository"
	"campusmart/wallet-service/internal/router"
	"campusmart/wallet-service/internal/service"
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

	if err := db.AutoMigrate(&model.UserWallet{}, &model.WalletFlow{}); err != nil {
		logger.Fatalf("migrate db: %v", err)
	}

	walletRepo := repository.NewWalletRepository(db)
	walletSvc := service.NewWalletService(walletRepo)

	workerCtx, stopWorkers := context.WithCancel(context.Background())
	defer stopWorkers()
	rabbitClient, err := mq.NewClient(cfg.RabbitMQURL, logger)
	if err != nil {
		logger.Printf("rabbitmq disabled: %v", err)
	} else {
		defer rabbitClient.Close()
		if err := rabbitClient.StartConsumers(workerCtx, walletSvc); err != nil {
			logger.Printf("start wallet consumers failed: %v", err)
		}
	}

	walletHandler := handler.NewWalletHandler(walletSvc, cfg.JWTSecret)
	r := gin.New()
	r.Use(gin.Recovery())
	r.Use(middleware.CORS())
	router.Register(r, walletHandler)

	addr := ":" + cfg.Port
	srv := &http.Server{
		Addr:              addr,
		Handler:           r,
		ReadHeaderTimeout: 10 * time.Second,
	}
	go func() {
		logger.Printf("wallet-service listening on %s", addr)
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
