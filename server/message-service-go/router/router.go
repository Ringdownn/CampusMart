package router

import (
	"campusmart/message-service-go/controller"
	"github.com/gin-gonic/gin"
)

func Register(r *gin.Engine, messageController *controller.MessageController, wsHandler gin.HandlerFunc) {
	r.GET("/app/messages/list", messageController.List)
	r.GET("/app/messages/recent", messageController.Recent)
	r.GET("/ws", wsHandler)
}
