package handler

import (
	"errors"
	"net/http"
	"strconv"

	"campusmart/trade-service/internal/auth"
	"campusmart/trade-service/internal/repository"
	"campusmart/trade-service/internal/result"
	"campusmart/trade-service/internal/service"
	"github.com/gin-gonic/gin"
)

type OrderHandler struct {
	svc       *service.OrderService
	jwtSecret string
}

func NewOrderHandler(svc *service.OrderService, jwtSecret string) *OrderHandler {
	return &OrderHandler{svc: svc, jwtSecret: jwtSecret}
}

func (h *OrderHandler) CreateOrder(c *gin.Context) {
	userID, ok := h.currentUserID(c)
	if !ok {
		return
	}
	var req service.CreateOrderRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: "参数不正确", Data: nil})
		return
	}
	order, err := h.svc.CreateOrder(c.Request.Context(), userID, req)
	h.writeOrderResult(c, order, err)
}

func (h *OrderHandler) GetOrder(c *gin.Context) {
	userID, ok := h.currentUserID(c)
	if !ok {
		return
	}
	orderID, ok := orderIDParam(c)
	if !ok {
		return
	}
	order, err := h.svc.GetOrder(c.Request.Context(), userID, orderID)
	h.writeOrderResult(c, order, err)
}

func (h *OrderHandler) BuyerOrders(c *gin.Context) {
	userID, ok := h.currentUserID(c)
	if !ok {
		return
	}
	orders, err := h.svc.ListBuyerOrders(c.Request.Context(), userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, result.Error("查询买家订单失败"))
		return
	}
	c.JSON(http.StatusOK, result.OK(orders))
}

func (h *OrderHandler) SellerOrders(c *gin.Context) {
	userID, ok := h.currentUserID(c)
	if !ok {
		return
	}
	orders, err := h.svc.ListSellerOrders(c.Request.Context(), userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, result.Error("查询卖家订单失败"))
		return
	}
	c.JSON(http.StatusOK, result.OK(orders))
}

func (h *OrderHandler) CancelOrder(c *gin.Context) {
	userID, ok := h.currentUserID(c)
	if !ok {
		return
	}
	orderID, ok := orderIDParam(c)
	if !ok {
		return
	}
	order, err := h.svc.CancelOrder(c.Request.Context(), userID, orderID, "BUYER_CANCEL")
	h.writeOrderResult(c, order, err)
}

func (h *OrderHandler) ConfirmReceipt(c *gin.Context) {
	userID, ok := h.currentUserID(c)
	if !ok {
		return
	}
	orderID, ok := orderIDParam(c)
	if !ok {
		return
	}
	order, err := h.svc.ConfirmReceipt(c.Request.Context(), userID, orderID)
	h.writeOrderResult(c, order, err)
}

func (h *OrderHandler) currentUserID(c *gin.Context) (int64, bool) {
	userID, err := auth.CurrentUserID(c.Request, h.jwtSecret)
	if err != nil {
		c.JSON(http.StatusUnauthorized, result.Result[any]{Code: 201, Message: "未登录或登录已过期", Data: nil})
		return 0, false
	}
	return userID, true
}

func (h *OrderHandler) writeOrderResult(c *gin.Context, data interface{}, err error) {
	switch {
	case errors.Is(err, service.ErrInvalidParam):
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: err.Error(), Data: nil})
	case errors.Is(err, repository.ErrGoodsNotFound), errors.Is(err, service.ErrOrderNotFound):
		c.JSON(http.StatusNotFound, result.Result[any]{Code: 204, Message: err.Error(), Data: nil})
	case errors.Is(err, service.ErrOrderForbidden):
		c.JSON(http.StatusForbidden, result.Result[any]{Code: 201, Message: err.Error(), Data: nil})
	case errors.Is(err, repository.ErrBuyOwnGoods), errors.Is(err, repository.ErrGoodsAlreadyOrdered),
		errors.Is(err, service.ErrOrderNotPayable), errors.Is(err, service.ErrOrderNotSettleable):
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: err.Error(), Data: nil})
	case err != nil:
		c.JSON(http.StatusInternalServerError, result.Error("订单服务异常"))
	default:
		c.JSON(http.StatusOK, result.OK(data))
	}
}

func orderIDParam(c *gin.Context) (int64, bool) {
	orderID, err := strconv.ParseInt(c.Param("orderId"), 10, 64)
	if err != nil || orderID <= 0 {
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: "参数不正确", Data: nil})
		return 0, false
	}
	return orderID, true
}
