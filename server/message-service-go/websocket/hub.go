package websocket

import (
	"encoding/json"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

type Hub struct {
	register   chan *client
	unregister chan *client
	deliver    chan deliverReq

	mu      sync.RWMutex
	clients map[int64]map[*client]struct{}
}

type deliverReq struct {
	userID int64
	data   []byte
}

func NewHub() *Hub {
	return &Hub{
		register:   make(chan *client, 64),
		unregister: make(chan *client, 64),
		deliver:    make(chan deliverReq, 256),
		clients:    make(map[int64]map[*client]struct{}),
	}
}

func (h *Hub) Run() {
	for {
		select {
		case c := <-h.register:
			h.mu.Lock()
			set := h.clients[c.userID]
			if set == nil {
				set = make(map[*client]struct{})
				h.clients[c.userID] = set
			}
			set[c] = struct{}{}
			h.mu.Unlock()
		case c := <-h.unregister:
			h.mu.Lock()
			set := h.clients[c.userID]
			if set != nil {
				delete(set, c)
				if len(set) == 0 {
					delete(h.clients, c.userID)
				}
			}
			h.mu.Unlock()
		case req := <-h.deliver:
			h.mu.RLock()
			set := h.clients[req.userID]
			for c := range set {
				select {
				case c.send <- req.data:
				default:
					go func(cc *client) { h.unregister <- cc }(c)
				}
			}
			h.mu.RUnlock()
		}
	}
}

func (h *Hub) SendToUser(userID int64, payload any) {
	b, err := json.Marshal(payload)
	if err != nil {
		return
	}
	h.deliver <- deliverReq{userID: userID, data: b}
}

type client struct {
	userID int64
	conn   *websocket.Conn
	send   chan []byte
}

func (c *client) writePump(h *Hub) {
	ticker := time.NewTicker(45 * time.Second)
	defer func() {
		ticker.Stop()
		_ = c.conn.Close()
	}()

	for {
		select {
		case msg, ok := <-c.send:
			_ = c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if !ok {
				_ = c.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			if err := c.conn.WriteMessage(websocket.TextMessage, msg); err != nil {
				return
			}
		case <-ticker.C:
			_ = c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := c.conn.WriteMessage(websocket.PingMessage, []byte("ping")); err != nil {
				return
			}
		}
	}
}
