package router

import (
	"net/http"

	"campusmart/wallet-service/internal/handler"
	"github.com/gin-gonic/gin"
)

func Register(r *gin.Engine, walletHandler *handler.WalletHandler) {
	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	r.GET("/app/wallet", walletHandler.GetWallet)
	r.GET("/app/wallet/flows", walletHandler.ListFlows)
	r.POST("/app/wallet/withdraw", walletHandler.Withdraw)
}
