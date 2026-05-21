package router

import (
	"campusmart/search-engine/internal/web/controller"

	"github.com/gin-gonic/gin"
)

func InitBaseRouter(router *gin.RouterGroup) {
	baseRouter := router.Group("")
	{
		baseRouter.POST("query", controller.Query)
	}
}
