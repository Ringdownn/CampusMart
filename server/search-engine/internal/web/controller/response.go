package controller

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

type ResponseData struct {
	State   bool        `json:"state"`
	Message string      `json:"message,omitempty"`
	Data    interface{} `json:"data,omitempty"`
}

func ResponseSuccessWithData(c *gin.Context, data interface{}) {
	c.JSON(http.StatusOK, &ResponseData{
		State:   true,
		Message: "success",
		Data:    data,
	})
}

func ResponseErrorWithMessage(c *gin.Context, message string) {
	c.JSON(http.StatusOK, &ResponseData{
		State:   false,
		Message: message,
		Data:    nil,
	})
}

func ResponseSuccess(c *gin.Context) {
	c.JSON(http.StatusOK, &ResponseData{
		State:   true,
		Message: "success",
		Data:    nil,
	})
}
