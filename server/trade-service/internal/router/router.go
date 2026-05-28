package router

import (
	"net/http"

	"campusmart/trade-service/internal/handler"
	"github.com/gin-gonic/gin"
)

func Register(r *gin.Engine, orderHandler *handler.OrderHandler) {
	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	r.POST("/app/orders", orderHandler.CreateOrder)
	r.GET("/app/orders/buyer", orderHandler.BuyerOrders)
	r.GET("/app/orders/seller", orderHandler.SellerOrders)
	r.GET("/app/orders/:orderId", orderHandler.GetOrder)
	r.POST("/app/orders/:orderId/cancel", orderHandler.CancelOrder)
	r.POST("/app/orders/:orderId/confirm-receipt", orderHandler.ConfirmReceipt)
}
