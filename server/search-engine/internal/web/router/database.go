package router

import (
	"campusmart/search-engine/internal/web/controller"

	"github.com/gin-gonic/gin"
)

func InitDatabaseRouter(router *gin.RouterGroup) {
	databaseRouter := router.Group("db")
	{
		databaseRouter.GET("list", controller.DBS)
		databaseRouter.GET("drop", controller.DatabaseDrop)
		databaseRouter.GET("create", controller.DatabaseCreate)
	}
}
