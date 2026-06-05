package alipay

import (
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"errors"
	"net/url"
	"sort"
	"strings"
	"time"
)

const (
	MethodTradeAppPay = "alipay.trade.app.pay"
	ProductCodeAppPay = "QUICK_MSECURITY_PAY"
)

var (
	ErrMissingConfig = errors.New("alipay config is incomplete")
	ErrInvalidKey    = errors.New("invalid alipay key")
)

type Config struct {
	AppID         string
	AppPrivateKey string
	AlipayKey     string
	NotifyURL     string
	IsProduction  bool
}

type Client struct {
	cfg        Config
	privateKey *rsa.PrivateKey
	publicKey  *rsa.PublicKey
}

type AppPayOrder struct {
	OutTradeNo  string
	Subject     string
	TotalAmount string
	Body        string
}

type appPayBizContent struct {
	OutTradeNo  string `json:"out_trade_no"`
	Subject     string `json:"subject"`
	TotalAmount string `json:"total_amount"`
	ProductCode string `json:"product_code"`
	Body        string `json:"body,omitempty"`
}

func NewClient(cfg Config) (*Client, error) {
	if strings.TrimSpace(cfg.AppID) == "" ||
		strings.TrimSpace(cfg.AppPrivateKey) == "" ||
		strings.TrimSpace(cfg.AlipayKey) == "" ||
		strings.TrimSpace(cfg.NotifyURL) == "" {
		return nil, ErrMissingConfig
	}

	privateKey, err := parsePrivateKey(cfg.AppPrivateKey)
	if err != nil {
		return nil, err
	}
	publicKey, err := parsePublicKey(cfg.AlipayKey)
	if err != nil {
		return nil, err
	}
	return &Client{cfg: cfg, privateKey: privateKey, publicKey: publicKey}, nil
}

func (c *Client) BuildAppPayOrder(order AppPayOrder) (string, error) {
	bizContent, err := json.Marshal(appPayBizContent{
		OutTradeNo:  order.OutTradeNo,
		Subject:     order.Subject,
		TotalAmount: order.TotalAmount,
		ProductCode: ProductCodeAppPay,
		Body:        order.Body,
	})
	if err != nil {
		return "", err
	}

	params := map[string]string{
		"app_id":      c.cfg.AppID,
		"method":      MethodTradeAppPay,
		"format":      "json",
		"charset":     "utf-8",
		"sign_type":   "RSA2",
		"timestamp":   time.Now().Format("2006-01-02 15:04:05"),
		"version":     "1.0",
		"notify_url":  c.cfg.NotifyURL,
		"biz_content": string(bizContent),
	}

	sign, err := c.sign(params)
	if err != nil {
		return "", err
	}
	params["sign"] = sign
	return encodeParams(params), nil
}

func (c *Client) VerifyNotify(params map[string]string) bool {
	sign := strings.TrimSpace(params["sign"])
	if sign == "" || strings.TrimSpace(params["sign_type"]) != "RSA2" {
		return false
	}

	source := signSource(params, "sign", "sign_type")
	digest := sha256.Sum256([]byte(source))
	signature, err := base64.StdEncoding.DecodeString(sign)
	if err != nil {
		return false
	}
	return rsa.VerifyPKCS1v15(c.publicKey, crypto.SHA256, digest[:], signature) == nil
}

func (c *Client) sign(params map[string]string) (string, error) {
	source := signSource(params)
	digest := sha256.Sum256([]byte(source))
	signature, err := rsa.SignPKCS1v15(rand.Reader, c.privateKey, crypto.SHA256, digest[:])
	if err != nil {
		return "", err
	}
	return base64.StdEncoding.EncodeToString(signature), nil
}

func signSource(params map[string]string, excluded ...string) string {
	excludes := make(map[string]struct{}, len(excluded))
	for _, key := range excluded {
		excludes[key] = struct{}{}
	}

	keys := make([]string, 0, len(params))
	for key, value := range params {
		if _, ok := excludes[key]; ok || value == "" {
			continue
		}
		keys = append(keys, key)
	}
	sort.Strings(keys)

	parts := make([]string, 0, len(keys))
	for _, key := range keys {
		parts = append(parts, key+"="+params[key])
	}
	return strings.Join(parts, "&")
}

func encodeParams(params map[string]string) string {
	values := url.Values{}
	for key, value := range params {
		values.Set(key, value)
	}
	return values.Encode()
}

func parsePrivateKey(raw string) (*rsa.PrivateKey, error) {
	block, err := decodePEM(raw, "PRIVATE KEY")
	if err != nil {
		return nil, err
	}
	if key, err := x509.ParsePKCS1PrivateKey(block.Bytes); err == nil {
		return key, nil
	}
	parsed, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		return nil, ErrInvalidKey
	}
	key, ok := parsed.(*rsa.PrivateKey)
	if !ok {
		return nil, ErrInvalidKey
	}
	return key, nil
}

func parsePublicKey(raw string) (*rsa.PublicKey, error) {
	block, err := decodePEM(raw, "PUBLIC KEY")
	if err != nil {
		return nil, err
	}
	parsed, err := x509.ParsePKIXPublicKey(block.Bytes)
	if err == nil {
		key, ok := parsed.(*rsa.PublicKey)
		if !ok {
			return nil, ErrInvalidKey
		}
		return key, nil
	}
	key, err := x509.ParsePKCS1PublicKey(block.Bytes)
	if err != nil {
		return nil, ErrInvalidKey
	}
	return key, nil
}

func decodePEM(raw string, blockType string) (*pem.Block, error) {
	normalized := normalizeKey(raw)
	if !strings.Contains(normalized, "-----BEGIN") {
		normalized = "-----BEGIN " + blockType + "-----\n" + normalized + "\n-----END " + blockType + "-----"
	}
	block, _ := pem.Decode([]byte(normalized))
	if block == nil {
		return nil, ErrInvalidKey
	}
	return block, nil
}

func normalizeKey(raw string) string {
	value := strings.TrimSpace(raw)
	value = strings.ReplaceAll(value, "\\n", "\n")
	return value
}
