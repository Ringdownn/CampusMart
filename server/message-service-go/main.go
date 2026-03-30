package main

import (
	"context"
	"database/sql"
	"errors"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"

	"campusmart/message-service-go/controller"
	"campusmart/message-service-go/middleware"
	"campusmart/message-service-go/router"
	"campusmart/message-service-go/service"
	"campusmart/message-service-go/websocket"

	"github.com/gin-gonic/gin"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
)

func main() {
	logger := log.New(os.Stdout, "", log.LstdFlags)

	db, sqlDB, err := openDBFromEnv()
	if err != nil {
		logger.Fatalf("open db: %v", err)
	}
	defer sqlDB.Close()

	messageRepo := service.NewMessageRepository(db)
	hub := websocket.NewHub()
	go hub.Run()

	r := gin.New()
	r.Use(gin.Recovery())
	r.Use(middleware.CORS())

	querySvc := service.NewMessageService(db)
	messageController := controller.NewMessageController(querySvc)

	wsHandler := websocket.NewHTTPHandler(hub, messageRepo, logger)
	router.Register(r, messageController, func(c *gin.Context) {
		wsHandler(c.Writer, c.Request)
	})

	addr := ":" + envOrDefault("PORT", "8083")
	srv := &http.Server{
		Addr:              addr,
		Handler:           r,
		ReadHeaderTimeout: 10 * time.Second,
	}

	errCh := make(chan error, 1)
	go func() {
		logger.Printf("listening on %s", addr)
		if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			errCh <- err
		}
	}()

	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)

	select {
	case sig := <-sigCh:
		logger.Printf("signal: %s", sig.String())
	case err := <-errCh:
		logger.Printf("server error: %v", err)
	}

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(ctx)
}

func openDBFromEnv() (*gorm.DB, *sql.DB, error) {
	dsn := os.Getenv("MESSAGE_DB_DSN")
	if dsn == "" {
		return nil, nil, errors.New("MESSAGE_DB_DSN environment variable is required")
	}

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

func envOrDefault(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

func parseLongQueryParam(r *http.Request, name string) (int64, bool) {
	v := r.URL.Query().Get(name)
	if v == "" {
		return 0, false
	}
	n, err := strconv.ParseInt(v, 10, 64)
	if err != nil {
		return 0, false
	}
	return n, true
}
