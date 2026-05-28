package handler

import (
	"errors"
	"net/http"
	"strconv"

	"campusmart/wallet-service/internal/auth"
	"campusmart/wallet-service/internal/repository"
	"campusmart/wallet-service/internal/result"
	"campusmart/wallet-service/internal/service"
	"github.com/gin-gonic/gin"
)

type WalletHandler struct {
	svc       *service.WalletService
	jwtSecret string
}

func NewWalletHandler(svc *service.WalletService, jwtSecret string) *WalletHandler {
	return &WalletHandler{svc: svc, jwtSecret: jwtSecret}
}

func (h *WalletHandler) GetWallet(c *gin.Context) {
	userID, ok := h.currentUserID(c)
	if !ok {
		return
	}
	wallet, err := h.svc.GetWallet(c.Request.Context(), userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, result.Error("查询钱包失败"))
		return
	}
	c.JSON(http.StatusOK, result.OK(wallet))
}

func (h *WalletHandler) ListFlows(c *gin.Context) {
	userID, ok := h.currentUserID(c)
	if !ok {
		return
	}
	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "50"))
	flows, err := h.svc.ListFlows(c.Request.Context(), userID, limit)
	if err != nil {
		c.JSON(http.StatusInternalServerError, result.Error("查询钱包流水失败"))
		return
	}
	c.JSON(http.StatusOK, result.OK(flows))
}

func (h *WalletHandler) Withdraw(c *gin.Context) {
	userID, ok := h.currentUserID(c)
	if !ok {
		return
	}
	var req service.WithdrawRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: "参数不正确", Data: nil})
		return
	}
	resp, err := h.svc.Withdraw(c.Request.Context(), userID, req)
	switch {
	case errors.Is(err, service.ErrInvalidParam), errors.Is(err, repository.ErrInvalidAmount):
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: err.Error(), Data: nil})
	case errors.Is(err, repository.ErrInsufficientBalance):
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: err.Error(), Data: nil})
	case err != nil:
		c.JSON(http.StatusInternalServerError, result.Error("模拟提现失败"))
	default:
		c.JSON(http.StatusOK, result.OK(resp))
	}
}

func (h *WalletHandler) currentUserID(c *gin.Context) (int64, bool) {
	userID, err := auth.CurrentUserID(c.Request, h.jwtSecret)
	if err != nil {
		c.JSON(http.StatusUnauthorized, result.Result[any]{Code: 201, Message: "未登录或登录已过期", Data: nil})
		return 0, false
	}
	return userID, true
}
