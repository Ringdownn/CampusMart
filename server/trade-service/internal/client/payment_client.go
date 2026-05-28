package client

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"strconv"
	"strings"
	"time"
)

type PaymentClient struct {
	baseURL string
	client  *http.Client
}

func NewPaymentClient(baseURL string) *PaymentClient {
	return &PaymentClient{
		baseURL: strings.TrimRight(baseURL, "/"),
		client:  &http.Client{Timeout: 5 * time.Second},
	}
}

type CreatePaymentRequest struct {
	OrderID int64  `json:"orderId"`
	OrderNo string `json:"orderNo"`
	BuyerID int64  `json:"buyerId"`
	Amount  string `json:"amount"`
}

type CreatePaymentResponse struct {
	OrderID int64  `json:"orderId"`
	OrderNo string `json:"orderNo"`
	PayNo   string `json:"payNo"`
	Status  string `json:"status"`
	Amount  string `json:"amount"`
}

type apiResult[T any] struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
	Data    T      `json:"data"`
}

func (c *PaymentClient) CreatePayment(ctx context.Context, req CreatePaymentRequest) (*CreatePaymentResponse, error) {
	var resp apiResult[CreatePaymentResponse]
	if err := c.postJSON(ctx, "/internal/payments", req, &resp); err != nil {
		return nil, err
	}
	if resp.Code != 200 {
		return nil, errors.New(resp.Message)
	}
	return &resp.Data, nil
}

func (c *PaymentClient) ClosePayment(ctx context.Context, orderID int64) error {
	var resp apiResult[map[string]interface{}]
	if err := c.postJSON(ctx, "/internal/payments/"+formatInt(orderID)+"/close", map[string]interface{}{}, &resp); err != nil {
		return err
	}
	if resp.Code != 200 && resp.Code != 204 {
		return errors.New(resp.Message)
	}
	return nil
}

func (c *PaymentClient) postJSON(ctx context.Context, path string, reqBody interface{}, respBody interface{}) error {
	body, err := json.Marshal(reqBody)
	if err != nil {
		return err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.baseURL+path, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := c.client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 500 {
		return errors.New("payment-service unavailable")
	}
	return json.NewDecoder(resp.Body).Decode(respBody)
}

func formatInt(value int64) string {
	return strconv.FormatInt(value, 10)
}
