package auth

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"net/http"
	"strconv"
	"strings"
	"time"
)

var ErrUnauthorized = errors.New("unauthorized")

type UserClaims struct {
	UserID   int64  `json:"userId"`
	Username string `json:"username"`
	Exp      int64  `json:"exp"`
}

func CurrentUserID(r *http.Request, jwtSecret string) (int64, error) {
	if userID, ok := userIDFromHeader(r); ok {
		return userID, nil
	}

	tokenText := strings.TrimSpace(r.Header.Get("access-token"))
	if tokenText == "" {
		tokenText = strings.TrimPrefix(strings.TrimSpace(r.Header.Get("Authorization")), "Bearer ")
	}
	if tokenText == "" {
		return 0, ErrUnauthorized
	}

	claims, err := parseHS256Token(tokenText, jwtSecret)
	if err != nil || claims.UserID <= 0 {
		return 0, ErrUnauthorized
	}
	return claims.UserID, nil
}

func parseHS256Token(tokenText, secret string) (*UserClaims, error) {
	parts := strings.Split(tokenText, ".")
	if len(parts) != 3 {
		return nil, ErrUnauthorized
	}

	signingInput := parts[0] + "." + parts[1]
	mac := hmac.New(sha256.New, []byte(secret))
	_, _ = mac.Write([]byte(signingInput))
	expectedSignature := mac.Sum(nil)

	signature, err := base64.RawURLEncoding.DecodeString(parts[2])
	if err != nil || !hmac.Equal(signature, expectedSignature) {
		return nil, ErrUnauthorized
	}

	payload, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return nil, ErrUnauthorized
	}

	var claims UserClaims
	if err := json.Unmarshal(payload, &claims); err != nil {
		return nil, ErrUnauthorized
	}
	if claims.Exp > 0 && time.Now().Unix() >= claims.Exp {
		return nil, ErrUnauthorized
	}
	return &claims, nil
}

func userIDFromHeader(r *http.Request) (int64, bool) {
	value := strings.TrimSpace(r.Header.Get("X-User-Id"))
	if value == "" {
		return 0, false
	}
	userID, err := strconv.ParseInt(value, 10, 64)
	if err != nil || userID <= 0 {
		return 0, false
	}
	return userID, true
}
