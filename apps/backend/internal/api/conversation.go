package api

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/middleware"
)

type ConversationHandler struct {
	conversationDAO *dao.ConversationDAO
}

func NewConversationHandler() *ConversationHandler {
	return &ConversationHandler{
		conversationDAO: dao.NewConversationDAO(),
	}
}

// GetConversations 获取用户的会话列表
func (h *ConversationHandler) GetConversations(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)

	conversations, err := h.conversationDAO.GetUserConversations(userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}

	// 转换为响应格式（包含完整数据）
	items := make([]map[string]interface{}, len(conversations))
	for i, conv := range conversations {
		// ✅ 计算真实未读数（不再是硬编码0）
		unreadCount := h.conversationDAO.GetUnreadCount(conv.ID, userID)
		
		item := map[string]interface{}{
			"id":            conv.ID,
			"type":          conv.Type,
			"unread_count":  unreadCount,          // ✅ 真实计算的未读数
			"updated_at":    conv.UpdatedAt.Unix(),
			"last_message":  conv.LastMessage,     // ✅ 完整的最后一条消息
		}
		
		// ✅ 添加对方用户信息（单聊）
		if conv.Type == "single" {
			if conv.User1ID != nil && *conv.User1ID != userID {
				item["user"] = conv.User1
			} else if conv.User2ID != nil {
				item["user"] = conv.User2
			}
		} else if conv.Type == "group" && conv.Group != nil {
			// ✅ 群聊信息
			item["group"] = conv.Group
		}
		
		items[i] = item
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"conversations": items,
		},
	})
}

// UpdateConversationSettings 更新会话设置
// PUT /conversations/:id/settings
// Body: {"is_muted": true, "is_top": false, "is_starred": true, "is_blocked": false}
func (h *ConversationHandler) UpdateConversationSettings(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	conversationID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid conversation ID",
			"data":    nil,
		})
		return
	}
	
	var req struct {
		IsMuted   *bool `json:"is_muted"`
		IsTop     *bool `json:"is_top"`
		IsStarred *bool `json:"is_starred"`
		IsBlocked *bool `json:"is_blocked"`
	}
	
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request parameters",
			"data":    nil,
		})
		return
	}
	
	// 构建更新字段
	settings := make(map[string]interface{})
	if req.IsMuted != nil {
		settings["is_muted"] = *req.IsMuted
	}
	if req.IsTop != nil {
		settings["is_top"] = *req.IsTop
	}
	if req.IsStarred != nil {
		settings["is_starred"] = *req.IsStarred
	}
	if req.IsBlocked != nil {
		settings["is_blocked"] = *req.IsBlocked
	}
	
	if len(settings) == 0 {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "No settings to update",
			"data":    nil,
		})
		return
	}
	
	// 更新设置
	if err := h.conversationDAO.UpdateSettings(uint(conversationID), userID, settings); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to update settings",
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Settings updated successfully",
		"data":    nil,
	})
}

// GetConversationSettings 获取会话设置
// GET /conversations/:id/settings
func (h *ConversationHandler) GetConversationSettings(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	conversationID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid conversation ID",
			"data":    nil,
		})
		return
	}
	
	settings, err := h.conversationDAO.GetConversationSettings(uint(conversationID), userID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Conversation not found",
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"is_muted":   settings.IsMuted,
			"is_top":     settings.IsTop,
			"is_starred": settings.IsStarred,
			"is_blocked": settings.IsBlocked,
		},
	})
}

