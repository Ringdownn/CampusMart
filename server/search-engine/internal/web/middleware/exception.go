package middleware

import (
	"campusmart/search-engine/internal/web"
	"net/http"
	"runtime/debug"

	"github.com/gin-gonic/gin"
)

func Exception() gin.HandlerFunc {
	return func(c *gin.Context) {
		defer func() {
			if err := recover(); err != nil {
				debug.PrintStack()
				c.JSON(http.StatusOK, web.Error(err.(error).Error()))
			}
			c.Abort()
		}()
		c.Next()
	}
}
