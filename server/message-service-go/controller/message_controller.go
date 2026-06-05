package controller

import (
	"context"
	"net/http"
	"strconv"
	"time"

	"campusmart/message-service-go/result"
	"campusmart/message-service-go/service"
	"github.com/gin-gonic/gin"
)

type MessageController struct {
	svc *service.MessageService
}

func NewMessageController(svc *service.MessageService) *MessageController {
	return &MessageController{svc: svc}
}

func (mc *MessageController) List(c *gin.Context) {
	senderID, err1 := strconv.ParseInt(c.Query("senderId"), 10, 64)
	receiverID, err2 := strconv.ParseInt(c.Query("receiverId"), 10, 64)
	goodID, err3 := strconv.ParseInt(c.Query("goodId"), 10, 64)
	if err1 != nil || err2 != nil || err3 != nil || senderID <= 0 || receiverID <= 0 || goodID <= 0 {
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: "参数不正确", Data: nil})
		return
	}

	ctx, cancel := context.WithTimeout(c.Request.Context(), 3*time.Second)
	defer cancel()

	list, err := mc.svc.ListByConversation(ctx, senderID, receiverID, goodID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, result.Result[any]{Code: 203, Message: "服务异常", Data: nil})
		return
	}

	c.JSON(http.StatusOK, result.Result[any]{Code: 200, Message: "成功", Data: list})
}

func (mc *MessageController) Recent(c *gin.Context) {
	userID, err := strconv.ParseInt(c.Query("userId"), 10, 64)
	if err != nil || userID <= 0 {
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: "参数不正确", Data: nil})
		return
	}

	ctx, cancel := context.WithTimeout(c.Request.Context(), 3*time.Second)
	defer cancel()

	list, err := mc.svc.RecentChats(ctx, userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, result.Result[any]{Code: 203, Message: "服务异常", Data: nil})
		return
	}

	c.JSON(http.StatusOK, result.Result[any]{Code: 200, Message: "成功", Data: list})
}
