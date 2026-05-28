package router

import (
	"net/http"

	"campusmart/payment-service/internal/handler"
	"github.com/gin-gonic/gin"
)

func Register(r *gin.Engine, alipayBindHandler *handler.AlipayBindHandler, paymentHandler *handler.PaymentHandler) {
	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	r.POST("/app/alipay/bind/mock", alipayBindHandler.BindMock)
	r.GET("/app/alipay/bind", alipayBindHandler.GetBind)
	r.DELETE("/app/alipay/bind", alipayBindHandler.Unbind)

	r.GET("/internal/alipay/binds/:userId", alipayBindHandler.InternalGetBind)

	r.POST("/internal/payments", paymentHandler.CreatePayment)
	r.POST("/internal/payments/:orderId/close", paymentHandler.ClosePayment)
	r.POST("/app/orders/:orderId/pay/alipay", paymentHandler.BuildAlipayOrderString)
	r.POST("/app/payments/alipay/notify", paymentHandler.AlipayNotify)
}
