package websocket

import (
	"encoding/json"
	"log"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
	"github.com/lanxin/im-backend/internal/pkg/jwt"
)

const (
	// 写入超时时间
	writeWait = 10 * time.Second

	// Pong等待时间必须大于ping时间
	pongWait = 60 * time.Second

	// Ping周期，必须小于pongWait
	pingPeriod = (pongWait * 9) / 10

	// 最大消息大小
	maxMessageSize = 10240 // 10KB
)

// 允许的WebSocket Origin列表（生产环境配置）
var allowedOrigins = []string{
	"https://app.lanxin168.com",
	"https://admin.lanxin168.com",
	"http://localhost:3000",  // 开发环境
	"http://localhost:8080",  // 开发环境
}

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		// ✅ 生产环境origin检查
		origin := r.Header.Get("Origin")
		
		// 如果没有Origin头（移动端APP通常没有），允许通过
		if origin == "" {
			return true
		}
		
		// 检查origin是否在白名单中
		for _, allowed := range allowedOrigins {
			if origin == allowed {
				return true
			}
		}
		
		// 开发模式：允许localhost
		if origin == "http://localhost:3000" || origin == "http://localhost:8080" {
			return true
		}
		
		// 其他origin拒绝
		log.Printf("Rejected WebSocket connection from origin: %s", origin)
		return false
	},
}

// Client 代表一个WebSocket客户端
type Client struct {
	hub *Hub

	// WebSocket连接
	conn *websocket.Conn

	// 发送消息的通道
	send chan []byte

	// 用户ID
	userID uint

	// 用户名
	username string

	deviceType string
}

// readPump 从WebSocket连接读取消息并发送到hub
func (c *Client) readPump() {
	defer func() {
		c.hub.unregister <- c
		c.conn.Close()
	}()

	c.conn.SetReadLimit(maxMessageSize)
	c.conn.SetReadDeadline(time.Now().Add(pongWait))
	c.conn.SetPongHandler(func(string) error {
		c.conn.SetReadDeadline(time.Now().Add(pongWait))
		return nil
	})

	for {
		_, message, err := c.conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				log.Printf("WebSocket error: %v", err)
			}
			break
		}

		// 处理客户端消息
		c.handleMessage(message)
	}
}

// writePump 从hub接收消息并写入WebSocket连接
func (c *Client) writePump() {
	ticker := time.NewTicker(pingPeriod)
	defer func() {
		ticker.Stop()
		c.conn.Close()
	}()

	for {
		select {
		case message, ok := <-c.send:
			c.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if !ok {
				// Hub关闭了通道
				c.conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}

			w, err := c.conn.NextWriter(websocket.TextMessage)
			if err != nil {
				return
			}
			w.Write(message)

			// 将队列中的消息一起发送
			n := len(c.send)
			for i := 0; i < n; i++ {
				w.Write([]byte{'\n'})
				w.Write(<-c.send)
			}

			if err := w.Close(); err != nil {
				return
			}

		case <-ticker.C:
			c.conn.SetWriteDeadline(time.Now().Add(writeWait))
			if err := c.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

// handleMessage 处理客户端发来的消息
func (c *Client) handleMessage(message []byte) {
	var msg WebSocketMessage
	if err := json.Unmarshal(message, &msg); err != nil {
		log.Printf("Error unmarshaling message: %v", err)
		return
	}

	switch msg.Type {
	case "ping":
		// 响应心跳
		pong := WebSocketMessage{
			Type: "pong",
			Data: map[string]interface{}{
				"timestamp": time.Now().Unix(),
			},
		}
		data, _ := json.Marshal(pong)
		c.send <- data

	default:
		log.Printf("Unknown message type: %s", msg.Type)
	}
}

// ServeWS 处理WebSocket请求
func ServeWS(hub *Hub, c *gin.Context, secret string) {
	// 从查询参数获取token
	tokenString := c.Query("token")
	if tokenString == "" {
		c.JSON(401, gin.H{"code": 401, "message": "Token required"})
		return
	}

	// 验证JWT
	claims, err := jwt.ParseToken(tokenString, secret)
	if err != nil {
		c.JSON(401, gin.H{"code": 401, "message": "Invalid token"})
		return
	}

	deviceType := c.Query("device_type")
	if deviceType == "" {
		userAgent := c.Request.Header.Get("User-Agent")
		if userAgent != "" {
			if contains(userAgent, "Android") {
				deviceType = "Android"
			} else if contains(userAgent, "iPhone") || contains(userAgent, "iPad") {
				deviceType = "iOS"
			} else {
				deviceType = "Web"
			}
		} else {
			deviceType = "Unknown"
		}
	}

	// 升级HTTP连接为WebSocket
	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		log.Printf("Failed to upgrade connection: %v", err)
		return
	}

	client := &Client{
		hub:        hub,
		conn:       conn,
		send:       make(chan []byte, 256),
		userID:     claims.UserID,
		username:   claims.Username,
		deviceType: deviceType,
	}

	client.hub.register <- client

	// 在新的goroutine中运行读写循环
	go client.writePump()
	go client.readPump()
}

func contains(s, substr string) bool {
	return len(s) >= len(substr) && (s == substr || len(s) > len(substr) && (s[:len(substr)] == substr || s[len(s)-len(substr):] == substr || indexContains(s, substr)))
}

func indexContains(s, substr string) bool {
	for i := 0; i <= len(s)-len(substr); i++ {
		if s[i:i+len(substr)] == substr {
			return true
		}
	}
	return false
}

