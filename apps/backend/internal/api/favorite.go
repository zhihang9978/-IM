package api

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/middleware"
	"github.com/lanxin/im-backend/internal/model"
)

type FavoriteHandler struct {
	favoriteDAO *dao.FavoriteDAO
	messageDAO  *dao.MessageDAO
	logDAO      *dao.OperationLogDAO
}

func NewFavoriteHandler() *FavoriteHandler {
	return &FavoriteHandler{
		favoriteDAO: dao.NewFavoriteDAO(),
		messageDAO:  dao.NewMessageDAO(),
		logDAO:      dao.NewOperationLogDAO(),
	}
}

// CollectMessage 收藏消息
// POST /messages/collect
// Body: {"message_id": 123}
func (h *FavoriteHandler) CollectMessage(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	
	var req struct {
		MessageID uint `json:"message_id" binding:"required"`
	}
	
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request parameters",
			"data":    nil,
		})
		return
	}
	
	// 检查消息是否存在
	message, err := h.messageDAO.GetByID(req.MessageID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Message not found",
			"data":    nil,
		})
		return
	}
	
	// 检查是否已收藏
	if h.favoriteDAO.CheckExists(userID, req.MessageID) {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Message already collected",
			"data":    nil,
		})
		return
	}
	
	// 创建收藏记录
	favorite := &model.Favorite{
		UserID:    userID,
		MessageID: req.MessageID,
		Content:   message.Content,
		Type:      message.Type,
	}
	
	if err := h.favoriteDAO.Create(favorite); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to collect message",
			"data":    nil,
		})
		return
	}
	
	// ✅ 记录操作日志
	h.logDAO.CreateLog(dao.LogRequest{
		Action: "message_collect",
		UserID: &userID,
		IP:     c.ClientIP(),
		UserAgent: c.GetHeader("User-Agent"),
		Details: map[string]interface{}{
			"message_id": req.MessageID,
			"type":       message.Type,
		},
		Result: model.ResultSuccess,
	})
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Message collected successfully",
		"data": gin.H{
			"favorite": favorite,
		},
	})
}

// GetFavorites 获取收藏列表
// GET /favorites
func (h *FavoriteHandler) GetFavorites(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))
	
	favorites, total, err := h.favoriteDAO.GetUserFavorites(userID, page, pageSize)
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
			"favorites": favorites,
		},
	})
}

// DeleteFavorite 删除收藏
// DELETE /favorites/:id
func (h *FavoriteHandler) DeleteFavorite(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	favoriteID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid favorite ID",
			"data":    nil,
		})
		return
	}
	
	// 验证收藏是否存在且属于当前用户
	_, err = h.favoriteDAO.GetByID(uint(favoriteID), userID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Favorite not found",
			"data":    nil,
		})
		return
	}
	
	// 删除
	if err := h.favoriteDAO.Delete(uint(favoriteID), userID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to delete favorite",
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Favorite deleted successfully",
		"data":    nil,
	})
}

