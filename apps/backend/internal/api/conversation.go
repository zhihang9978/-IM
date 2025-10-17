package api

import (
	"net/http"

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

