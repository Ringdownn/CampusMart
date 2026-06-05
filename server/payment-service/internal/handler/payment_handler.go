package handler

import (
	"encoding/json"
	"errors"
	"net/http"
	"sort"
	"strconv"
	"strings"

	"campusmart/payment-service/internal/result"
	"campusmart/payment-service/internal/service"
	"github.com/gin-gonic/gin"
)

type PaymentHandler struct {
	svc       *service.PaymentService
	jwtSecret string
}

func NewPaymentHandler(svc *service.PaymentService, jwtSecret string) *PaymentHandler {
	return &PaymentHandler{svc: svc, jwtSecret: jwtSecret}
}

func (h *PaymentHandler) CreatePayment(c *gin.Context) {
	var req service.CreatePaymentRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: "参数不正确", Data: nil})
		return
	}

	resp, err := h.svc.CreatePayment(c.Request.Context(), req)
	if errors.Is(err, service.ErrInvalidPaymentParam) {
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: err.Error(), Data: nil})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, result.Error("创建支付单失败"))
		return
	}
	c.JSON(http.StatusOK, result.OK(resp))
}

func (h *PaymentHandler) BuildAlipayOrderString(c *gin.Context) {
	userID, ok := currentUserID(c, h.jwtSecret)
	if !ok {
		return
	}
	orderID, err := strconv.ParseInt(c.Param("orderId"), 10, 64)
	if err != nil || orderID <= 0 {
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: "参数不正确", Data: nil})
		return
	}

	resp, err := h.svc.BuildAlipayOrderString(c.Request.Context(), orderID, userID)
	switch {
	case errors.Is(err, service.ErrPaymentNotFound):
		c.JSON(http.StatusNotFound, result.Result[any]{Code: 204, Message: err.Error(), Data: nil})
	case errors.Is(err, service.ErrPaymentForbidden):
		c.JSON(http.StatusForbidden, result.Result[any]{Code: 201, Message: err.Error(), Data: nil})
	case errors.Is(err, service.ErrPaymentNotPayable), errors.Is(err, service.ErrAlipayNotBound):
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: err.Error(), Data: nil})
	case err != nil:
		c.JSON(http.StatusInternalServerError, result.Error("生成支付宝沙箱支付参数失败"))
	default:
		c.JSON(http.StatusOK, result.OK(resp))
	}
}

func (h *PaymentHandler) AlipayNotify(c *gin.Context) {
	req, err := parseAlipayNotify(c)
	if err != nil {
		c.String(http.StatusBadRequest, "failure")
		return
	}

	ok, err := h.svc.HandleAlipayNotify(c.Request.Context(), req)
	if err != nil || !ok {
		c.String(http.StatusOK, "failure")
		return
	}
	c.String(http.StatusOK, "success")
}

func (h *PaymentHandler) ClosePayment(c *gin.Context) {
	orderID, err := strconv.ParseInt(c.Param("orderId"), 10, 64)
	if err != nil || orderID <= 0 {
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: "参数不正确", Data: nil})
		return
	}

	resp, err := h.svc.ClosePayment(c.Request.Context(), orderID)
	switch {
	case errors.Is(err, service.ErrPaymentNotFound):
		c.JSON(http.StatusNotFound, result.Result[any]{Code: 204, Message: err.Error(), Data: nil})
	case errors.Is(err, service.ErrInvalidPaymentParam):
		c.JSON(http.StatusBadRequest, result.Result[any]{Code: 202, Message: err.Error(), Data: nil})
	case err != nil:
		c.JSON(http.StatusInternalServerError, result.Error("关闭支付单失败"))
	default:
		c.JSON(http.StatusOK, result.OK(resp))
	}
}

func parseAlipayNotify(c *gin.Context) (service.AlipayNotifyRequest, error) {
	contentType := c.GetHeader("Content-Type")
	if strings.Contains(contentType, "application/json") {
		body, err := c.GetRawData()
		if err != nil {
			return service.AlipayNotifyRequest{}, err
		}
		var req service.AlipayNotifyRequest
		if err := json.Unmarshal(body, &req); err != nil {
			return service.AlipayNotifyRequest{}, err
		}
		req.RawBody = string(body)
		req.RawSignSource = string(body)
		return req, nil
	}

	if err := c.Request.ParseForm(); err != nil {
		return service.AlipayNotifyRequest{}, err
	}
	form := c.Request.PostForm
	req := service.AlipayNotifyRequest{
		OutTradeNo:    form.Get("out_trade_no"),
		TradeNo:       form.Get("trade_no"),
		TotalAmount:   form.Get("total_amount"),
		TradeStatus:   form.Get("trade_status"),
		AppID:         form.Get("app_id"),
		Sign:          form.Get("sign"),
		RawBody:       form.Encode(),
		RawSignSource: signSource(form),
	}
	return req, nil
}

func signSource(values map[string][]string) string {
	keys := make([]string, 0, len(values))
	for key := range values {
		if key != "sign" {
			keys = append(keys, key)
		}
	}
	sort.Strings(keys)
	parts := make([]string, 0, len(keys))
	for _, key := range keys {
		for _, value := range values[key] {
			parts = append(parts, key+"="+value)
		}
	}
	return strings.Join(parts, "&")
}
