package router

import (
	"campusmart/search-engine/internal/global"
	"campusmart/search-engine/internal/web/middleware"
	"log"
	"mime"

	"github.com/gin-gonic/gin"
)

func SetupRouter() *gin.Engine {
	if global.CONFIG.Debug {
		gin.SetMode(gin.DebugMode)
	} else {
		gin.SetMode(gin.ReleaseMode)
	}

	router := gin.Default()

	err := mime.AddExtensionType(".js", "application/javascript")
	if err != nil {
		panic(err)
	}

	router.Use(middleware.Cors(), middleware.Exception())
	group := router.Group("/api")
	{
		InitBaseRouter(group)
		InitDatabaseRouter(group)
		InitIndexRouter(group)
	}
	log.Printf("API Url: \t http://%v/api", global.CONFIG.Addr)
	return router
}
