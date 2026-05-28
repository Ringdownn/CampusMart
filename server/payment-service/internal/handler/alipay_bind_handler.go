package handler

import (
	"errors"
	"net/http"
	"strconv"

	"campusmart/payment-service/internal/auth"
	"campusmart/payment-service/internal/result"
	"campusmart/payment-service/internal/service"
	"github.com/gin-gonic/gin"
)

type AlipayBindHandler struct {
	svc       *service.AlipayBindService
	jwtSecret string
}

func NewAlipayBindHandler(svc *service.AlipayBindService, jwtSecret string) *AlipayBindHandler {
	return &AlipayBindHandler{svc: svc, jwtSecret: jwtSecret}
}

func (h *AlipayBindHandler) BindMock(c *gin.Context) {
	userID, ok := h.currentUserID(c)
	if !ok {
		return
	}

	var req service.BindAlipayRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: "参数不正确", Data: nil})
		return
	}

	resp, err := h.svc.BindMock(c.Request.Context(), userID, req)
	if errors.Is(err, service.ErrInvalidAlipayUserID) {
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: err.Error(), Data: nil})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, result.Error("绑定支付宝沙箱账户失败"))
		return
	}
	c.JSON(http.StatusOK, result.OK(resp))
}

func (h *AlipayBindHandler) GetBind(c *gin.Context) {
	userID, ok := h.currentUserID(c)
	if !ok {
		return
	}

	resp, err := h.svc.GetBind(c.Request.Context(), userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, result.Error("查询支付宝沙箱绑定失败"))
		return
	}
	c.JSON(http.StatusOK, result.OK(resp))
}

func (h *AlipayBindHandler) Unbind(c *gin.Context) {
	userID, ok := h.currentUserID(c)
	if !ok {
		return
	}

	if err := h.svc.Unbind(c.Request.Context(), userID); err != nil {
		c.JSON(http.StatusInternalServerError, result.Error("解绑支付宝沙箱账户失败"))
		return
	}
	c.JSON(http.StatusOK, result.OK(gin.H{"unbound": true}))
}

func (h *AlipayBindHandler) InternalGetBind(c *gin.Context) {
	userID, err := strconv.ParseInt(c.Param("userId"), 10, 64)
	if err != nil || userID <= 0 {
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: "参数不正确", Data: nil})
		return
	}

	resp, err := h.svc.GetBind(c.Request.Context(), userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, result.Error("查询支付宝沙箱绑定失败"))
		return
	}
	c.JSON(http.StatusOK, result.OK(resp))
}

func (h *AlipayBindHandler) currentUserID(c *gin.Context) (int64, bool) {
	userID, err := auth.CurrentUserID(c.Request, h.jwtSecret)
	if err != nil {
		c.JSON(http.StatusUnauthorized, result.Result[any]{Code: 201, Message: "未登录或登录已过期", Data: nil})
		return 0, false
	}
	return userID, true
}
