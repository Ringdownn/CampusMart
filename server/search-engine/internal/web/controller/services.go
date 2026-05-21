package controller

import (
	"campusmart/search-engine/internal/queue/producer"
	"campusmart/search-engine/internal/web/service"
	"log"
)

var srv *Services

type Services struct {
	Base     *service.Base
	Database *service.Database
	Index    *service.Index
}

// NewServices 初始化所有服务，注入依赖
func NewServices(taggingProducer *producer.TaggingProducer) {
	srv = &Services{
		Base:     service.NewBase(),
		Database: service.NewDatabase(),
		Index:    service.NewIndex(taggingProducer),
	}
}

// GetServices 获取服务实例
func GetServices() *Services {
	if srv == nil {
		log.Fatal("Services not initialized")
	}
	return srv
}
