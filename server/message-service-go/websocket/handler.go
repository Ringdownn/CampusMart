package websocket

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gorilla/websocket"
)

type MessageWriter interface {
	Insert(ctx context.Context, senderID, receiverID, goodID int64, content string, sendTime time.Time) (int64, error)
}

type Message struct {
	MessageID      *int64 `json:"messageID,omitempty"`
	SenderID       int64  `json:"senderID"`
	ReceiverID     int64  `json:"receiverID"`
	GoodID         int64  `json:"goodID"`
	MessageContent string `json:"messageContent"`
	SendTime       string `json:"sendTime,omitempty"`
}

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool { return true },
}

func NewHTTPHandler(h *Hub, repo MessageWriter, logger *log.Logger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userID, ok := parseLongQueryParam(r, "userId")
		if !ok || userID <= 0 {
			http.Error(w, "invalid userId", http.StatusBadRequest)
			return
		}
		if !isForwardedUserAllowed(r, userID) {
			http.Error(w, "userId does not match token", http.StatusForbidden)
			return
		}

		conn, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			return
		}

		c := &client{
			userID: userID,
			conn:   conn,
			send:   make(chan []byte, 64),
		}
		h.register <- c

		go c.writePump(h)
		c.readPump(h, repo, logger)
	}
}

func (c *client) readPump(h *Hub, repo MessageWriter, logger *log.Logger) {
	defer func() {
		h.unregister <- c
		_ = c.conn.Close()
	}()

	c.conn.SetReadLimit(1 << 20)
	_ = c.conn.SetReadDeadline(time.Now().Add(90 * time.Second))
	c.conn.SetPongHandler(func(string) error {
		_ = c.conn.SetReadDeadline(time.Now().Add(90 * time.Second))
		return nil
	})

	for {
		_, data, err := c.conn.ReadMessage()
		if err != nil {
			return
		}

		var msg Message
		if err := json.Unmarshal(data, &msg); err != nil {
			continue
		}

		msg.MessageContent = strings.TrimSpace(msg.MessageContent)
		if msg.SenderID == 0 {
			msg.SenderID = c.userID
		}
		if msg.SenderID != c.userID || msg.ReceiverID == 0 || msg.GoodID == 0 || msg.MessageContent == "" {
			continue
		}

		sendTime := parseSendTime(msg.SendTime)
		ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
		id, err := repo.Insert(ctx, msg.SenderID, msg.ReceiverID, msg.GoodID, msg.MessageContent, sendTime)
		cancel()
		if err != nil {
			if logger != nil {
				logger.Printf("insert message: %v", err)
			}
			continue
		}

		msg.MessageID = &id
		msg.SendTime = sendTime.Format("2006-01-02T15:04:05.000")

		h.SendToUser(msg.ReceiverID, msg)
		h.SendToUser(msg.SenderID, msg)
	}
}

func parseSendTime(v string) time.Time {
	if strings.TrimSpace(v) == "" {
		return time.Now()
	}
	layout := "2006-01-02T15:04:05.000"
	if t, err := time.ParseInLocation(layout, v, time.Local); err == nil {
		return t
	}
	if t, err := time.Parse(time.RFC3339Nano, v); err == nil {
		return t
	}
	return time.Now()
}

func parseLongQueryParam(r *http.Request, name string) (int64, bool) {
	v := r.URL.Query().Get(name)
	if v == "" {
		return 0, false
	}
	n, err := strconv.ParseInt(v, 10, 64)
	if err != nil {
		return 0, false
	}
	return n, true
}

func isForwardedUserAllowed(r *http.Request, queryUserID int64) bool {
	forwardedUserID := strings.TrimSpace(r.Header.Get("X-User-Id"))
	if forwardedUserID == "" {
		// Direct local connections remain compatible; gateway-authenticated
		// connections always carry X-User-Id.
		return true
	}
	n, err := strconv.ParseInt(forwardedUserID, 10, 64)
	return err == nil && n == queryUserID
}
