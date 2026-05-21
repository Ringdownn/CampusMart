package router

import (
	"campusmart/search-engine/internal/web/controller"

	"github.com/gin-gonic/gin"
)

func InitIndexRouter(router *gin.RouterGroup) {
	indexRouter := router.Group("index")
	{
		indexRouter.POST("", controller.AddIndex)
		indexRouter.POST("batch", controller.BatchAddIndex)
		indexRouter.POST("remove", controller.RemoveIndex)
	}
}
