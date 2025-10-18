package api

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/middleware"
	"github.com/lanxin/im-backend/internal/service"
	"github.com/lanxin/im-backend/internal/websocket"
	"github.com/lanxin/im-backend/pkg/kafka"
)

type MessageHandler struct {
	messageService *service.MessageService
}

func NewMessageHandler(hub *websocket.Hub, producer *kafka.Producer) *MessageHandler {
	return &MessageHandler{
		messageService: service.NewMessageService(hub, producer),
	}
}

// SendMessage 发送消息
func (h *MessageHandler) SendMessage(c *gin.Context) {
	senderID, _ := middleware.GetUserID(c)

	var req struct {
		ReceiverID uint    `json:"receiver_id" binding:"required"`
		Content    string  `json:"content" binding:"required"`
		Type       string  `json:"type"` // text, image, voice, video, file
		FileURL    *string `json:"file_url"`
		FileSize   *int64  `json:"file_size"`
		Duration   *int    `json:"duration"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}

	if req.Type == "" {
		req.Type = "text"
	}

	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")

	message, err := h.messageService.SendMessage(
		senderID,
		req.ReceiverID,
		req.Content,
		req.Type,
		req.FileURL,
		req.FileSize,
		req.Duration,
		ip,
		userAgent,
	)

	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"message": message,
		},
	})
}

// RecallMessage 撤回消息
func (h *MessageHandler) RecallMessage(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	messageID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")

	if err := h.messageService.RecallMessage(uint(messageID), userID, ip, userAgent); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    nil,
	})
}

// GetMessages 获取消息历史
func (h *MessageHandler) GetMessages(c *gin.Context) {
	conversationID, _ := strconv.ParseUint(c.Param("id"), 10, 32)
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "50"))

	messages, total, err := h.messageService.GetMessages(uint(conversationID), page, pageSize)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"total":    total,
			"messages": messages,
		},
	})
}

// MarkAsRead 标记会话已读
func (h *MessageHandler) MarkAsRead(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	conversationID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	if err := h.messageService.MarkAsRead(uint(conversationID), userID); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    nil,
	})
}

// GetHistoryMessages 获取历史消息（API层）
// 路由: GET /api/v1/conversations/:id/messages/history
// 参数: 
//   - id (path): 会话ID
//   - before_message_id (query): 加载此消息之前的历史
//   - limit (query): 返回数量，默认20
// 返回: 
//   {code: 0, message: "success", data: {total: 20, messages: [...]}}
func (h *MessageHandler) GetHistoryMessages(c *gin.Context) {
	// 解析路径参数
	conversationID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid conversation ID",
			"data":    nil,
		})
		return
	}
	
	// 解析查询参数
	beforeMessageID, _ := strconv.ParseUint(c.Query("before_message_id"), 10, 32)
	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "20"))
	
	// 调用Service层
	messages, err := h.messageService.GetHistoryMessages(
		uint(conversationID),
		uint(beforeMessageID),
		limit,
	)
	
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}
	
	// 返回成功响应
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"total":    len(messages),
			"messages": messages,
		},
	})
}

// SearchMessages 搜索消息
// GET /messages/search?keyword=xxx&page=1&page_size=20
func (h *MessageHandler) SearchMessages(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	keyword := c.Query("keyword")
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))
	
	if keyword == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Keyword required",
			"data":    nil,
		})
		return
	}
	
	messages, total, err := h.messageService.SearchMessages(userID, keyword, page, pageSize)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"total":     total,
			"page":      page,
			"page_size": pageSize,
			"messages":  messages,
		},
	})
}

// GetOfflineMessages 获取离线消息
// GET /api/v1/messages/offline
func (h *MessageHandler) GetOfflineMessages(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	
	messages, err := h.messageService.GetOfflineMessages(userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"messages": messages,
			"count":    len(messages),
		},
	})
}

