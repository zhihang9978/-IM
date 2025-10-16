package websocket

import (
	"encoding/json"
	"log"
	"sync"
	"time"
)

// Hub 维护活跃的客户端集合并向客户端广播消息
type Hub struct {
	// 注册的客户端
	clients map[*Client]bool

	// 用户ID到客户端的映射（一个用户可能有多个设备）
	userClients map[uint][]*Client

	// 从客户端接收的消息
	broadcast chan []byte

	// 注册请求
	register chan *Client

	// 注销请求
	unregister chan *Client

	// 互斥锁
	mu sync.RWMutex
}

// NewHub 创建新的Hub
func NewHub() *Hub {
	return &Hub{
		clients:     make(map[*Client]bool),
		userClients: make(map[uint][]*Client),
		broadcast:   make(chan []byte, 256),
		register:    make(chan *Client),
		unregister:  make(chan *Client),
	}
}

// Run 启动Hub的主循环
func (h *Hub) Run() {
	for {
		select {
		case client := <-h.register:
			h.mu.Lock()
			h.clients[client] = true
			// 将客户端添加到用户映射
			h.userClients[client.userID] = append(h.userClients[client.userID], client)
			h.mu.Unlock()
			log.Printf("Client registered: UserID=%d, Total clients=%d", client.userID, len(h.clients))

		case client := <-h.unregister:
			if _, ok := h.clients[client]; ok {
				h.mu.Lock()
				delete(h.clients, client)
				close(client.send)
				// 从用户映射中移除
				h.removeClientFromUser(client)
				h.mu.Unlock()
				log.Printf("Client unregistered: UserID=%d, Total clients=%d", client.userID, len(h.clients))
			}

		case message := <-h.broadcast:
			h.mu.RLock()
			for client := range h.clients {
				select {
				case client.send <- message:
				default:
					close(client.send)
					delete(h.clients, client)
				}
			}
			h.mu.RUnlock()
		}
	}
}

// removeClientFromUser 从用户映射中移除客户端
func (h *Hub) removeClientFromUser(client *Client) {
	clients := h.userClients[client.userID]
	for i, c := range clients {
		if c == client {
			h.userClients[client.userID] = append(clients[:i], clients[i+1:]...)
			break
		}
	}
	// 如果该用户没有任何客户端连接，删除映射
	if len(h.userClients[client.userID]) == 0 {
		delete(h.userClients, client.userID)
	}
}

// SendToUser 向指定用户的所有设备发送消息
func (h *Hub) SendToUser(userID uint, message interface{}) error {
	data, err := json.Marshal(message)
	if err != nil {
		return err
	}

	h.mu.RLock()
	defer h.mu.RUnlock()

	clients, exists := h.userClients[userID]
	if !exists || len(clients) == 0 {
		log.Printf("User %d has no active connections", userID)
		return nil
	}

	for _, client := range clients {
		select {
		case client.send <- data:
		default:
			log.Printf("Failed to send message to client of user %d", userID)
		}
	}

	return nil
}

// BroadcastToAll 向所有连接的客户端广播消息
func (h *Hub) BroadcastToAll(message interface{}) error {
	data, err := json.Marshal(message)
	if err != nil {
		return err
	}

	h.broadcast <- data
	return nil
}

// GetOnlineUserCount 获取在线用户数量
func (h *Hub) GetOnlineUserCount() int {
	h.mu.RLock()
	defer h.mu.RUnlock()
	return len(h.userClients)
}

// GetTotalClientCount 获取总连接数（包括同一用户的多个设备）
func (h *Hub) GetTotalClientCount() int {
	h.mu.RLock()
	defer h.mu.RUnlock()
	return len(h.clients)
}

// IsUserOnline 检查用户是否在线
func (h *Hub) IsUserOnline(userID uint) bool {
	h.mu.RLock()
	defer h.mu.RUnlock()
	clients, exists := h.userClients[userID]
	return exists && len(clients) > 0
}

// WebSocketMessage WebSocket消息格式
type WebSocketMessage struct {
	Type string      `json:"type"`
	Data interface{} `json:"data"`
}

// SendMessageNotification 发送新消息通知
func (h *Hub) SendMessageNotification(userID uint, messageData interface{}) error {
	msg := WebSocketMessage{
		Type: "message",
		Data: messageData,
	}
	return h.SendToUser(userID, msg)
}

// SendMessageStatusUpdate 发送消息状态更新
func (h *Hub) SendMessageStatusUpdate(userID uint, messageID uint, status string) error {
	msg := WebSocketMessage{
		Type: "message_status",
		Data: map[string]interface{}{
			"message_id": messageID,
			"status":     status,
			"timestamp":  time.Now().Format(time.RFC3339),
		},
	}
	return h.SendToUser(userID, msg)
}

// SendCallInvite 发送通话邀请
func (h *Hub) SendCallInvite(userID uint, callData interface{}) error {
	msg := WebSocketMessage{
		Type: "call_invite",
		Data: callData,
	}
	return h.SendToUser(userID, msg)
}

