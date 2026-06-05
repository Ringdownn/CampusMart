package handler

import (
	"net/http"

	"campusmart/payment-service/internal/auth"
	"campusmart/payment-service/internal/result"
	"github.com/gin-gonic/gin"
)

func currentUserID(c *gin.Context, jwtSecret string) (int64, bool) {
	userID, err := auth.CurrentUserID(c.Request, jwtSecret)
	if err != nil {
		c.JSON(http.StatusUnauthorized, result.Result[any]{Code: 201, Message: "未登录或登录已过期", Data: nil})
		return 0, false
	}
	return userID, true
}
