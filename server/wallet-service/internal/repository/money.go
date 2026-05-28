package repository

import (
	"fmt"
	"strconv"
	"strings"
	"time"
)

func amountToCents(amount string) (int64, error) {
	amount = strings.TrimSpace(amount)
	if amount == "" {
		return 0, ErrInvalidAmount
	}
	parts := strings.SplitN(amount, ".", 2)
	yuan, err := strconv.ParseInt(parts[0], 10, 64)
	if err != nil || yuan < 0 {
		return 0, ErrInvalidAmount
	}
	cents := int64(0)
	if len(parts) == 2 {
		if len(parts[1]) > 2 {
			return 0, ErrInvalidAmount
		}
		decimal := parts[1]
		if len(decimal) == 1 {
			decimal += "0"
		}
		if decimal != "" {
			cents, err = strconv.ParseInt(decimal, 10, 64)
			if err != nil || cents < 0 {
				return 0, ErrInvalidAmount
			}
		}
	}
	return yuan*100 + cents, nil
}

func centsToAmount(cents int64) string {
	return fmt.Sprintf("%d.%02d", cents/100, cents%100)
}

func newFlowNo(prefix string) string {
	return prefix + time.Now().Format("20060102150405") + strconv.FormatInt(time.Now().UnixNano()%1_000_000, 10)
}
