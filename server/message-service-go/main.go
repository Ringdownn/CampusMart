package main

import (
	"context"
	"database/sql"
	"errors"
	"log"
	"net"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"strings"
	"syscall"
	"time"

	"campusmart/message-service-go/controller"
	"campusmart/message-service-go/middleware"
	"campusmart/message-service-go/router"
	"campusmart/message-service-go/service"
	"campusmart/message-service-go/websocket"

	"github.com/gin-gonic/gin"
	"github.com/nacos-group/nacos-sdk-go/v2/clients"
	"github.com/nacos-group/nacos-sdk-go/v2/common/constant"
	"github.com/nacos-group/nacos-sdk-go/v2/vo"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
)

func main() {
	logger := log.New(os.Stdout, "", log.LstdFlags)

	minioEndpoint := envOrDefault("MINIO_ENDPOINT", "http://localhost:9000")
	minioBucket := envOrDefault("MINIO_BUCKET", "campusmart")

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

	querySvc := service.NewMessageService(db, minioEndpoint, minioBucket, logger)
	messageController := controller.NewMessageController(querySvc)

	wsHandler := websocket.NewHTTPHandler(hub, messageRepo, logger)
	router.Register(r, messageController, func(c *gin.Context) {
		wsHandler(c.Writer, c.Request)
	})

	port := envOrDefault("PORT", "8083")
	addr := ":" + port
	srv := &http.Server{
		Addr:              addr,
		Handler:           r,
		ReadHeaderTimeout: 10 * time.Second,
	}

	go func() {
		logger.Printf("listening on %s", addr)
		if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			logger.Printf("server error: %v", err)
		}
	}()

	registerToNacos(port, logger)

	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)

	sig := <-sigCh
	logger.Printf("signal: %s", sig.String())

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_ = srv.Shutdown(ctx)
}

func registerToNacos(port string, logger *log.Logger) {
	nacosServerAddr := os.Getenv("NACOS_SERVER_ADDR")
	if nacosServerAddr == "" {
		nacosServerAddr = "localhost:8848"
	}
	nacosHost, nacosPort := splitHostPort(nacosServerAddr, 8848)

	nacosNamespace := envOrDefault("NACOS_NAMESPACE", "campusmart")
	nacosGroup := envOrDefault("NACOS_GROUP", "DEFAULT_GROUP")

	sc := []constant.ServerConfig{
		{
			IpAddr: nacosHost,
			Port:   uint64(nacosPort),
		},
	}

	cc := constant.ClientConfig{
		NamespaceId: nacosNamespace,
		TimeoutMs:   5000,
		LogDir:      "/tmp/nacos/log",
		CacheDir:    "/tmp/nacos/cache",
	}

	client, err := clients.NewNamingClient(
		vo.NacosClientParam{
			ServerConfigs: sc,
			ClientConfig:  &cc,
		},
	)
	if err != nil {
		logger.Printf("failed to create nacos client: %v", err)
		return
	}

	instanceID := os.Getenv("HOSTNAME")
	if instanceID == "" {
		instanceID = "message-service-" + port
	}

	_, err = client.RegisterInstance(vo.RegisterInstanceParam{
		Ip:          getOutboundIP(),
		Port:        parsePort(port),
		ServiceName: "message-service",
		GroupName:   nacosGroup,
		Weight:      1,
		Enable:      true,
		Healthy:     true,
		Ephemeral:   true,
	})
	if err != nil {
		logger.Printf("failed to register to nacos: %v", err)
		return
	}

	logger.Printf("registered to nacos: message-service -> %s:%s", getOutboundIP(), port)
}

func splitHostPort(addr string, defaultPort uint64) (string, uint64) {
	host, port, err := net.SplitHostPort(addr)
	if err == nil {
		return host, parsePortOrDefault(port, defaultPort)
	}
	if h, p, ok := strings.Cut(addr, ":"); ok {
		return h, parsePortOrDefault(p, defaultPort)
	}
	return addr, defaultPort
}

func getOutboundIP() string {
	conn, err := net.Dial("udp", "8.8.8.8:80")
	if err != nil {
		return "127.0.0.1"
	}
	defer conn.Close()
	localAddr := conn.LocalAddr().(*net.UDPAddr)
	return localAddr.IP.String()
}

func parsePort(port string) uint64 {
	p, err := strconv.ParseUint(port, 10, 64)
	if err != nil {
		return 8083
	}
	return p
}

func parsePortOrDefault(port string, defaultPort uint64) uint64 {
	p, err := strconv.ParseUint(port, 10, 64)
	if err != nil {
		return defaultPort
	}
	return p
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
