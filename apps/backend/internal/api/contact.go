package api

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/middleware"
)

type ContactHandler struct {
	contactDAO *dao.ContactDAO
}

func NewContactHandler() *ContactHandler {
	return &ContactHandler{
		contactDAO: dao.NewContactDAO(),
	}
}

// GetContacts 获取用户的联系人列表
func (h *ContactHandler) GetContacts(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)

	contacts, err := h.contactDAO.GetUserContacts(userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}

	// 转换为响应格式
	items := make([]map[string]interface{}, len(contacts))
	for i, contact := range contacts {
		items[i] = map[string]interface{}{
			"id":         contact.ID,
			"user_id":    contact.UserID,
			"contact_id": contact.ContactID,
			"remark":     contact.Remark,
			"tags":       contact.Tags,
			"status":     contact.Status,
			"created_at": contact.CreatedAt.Unix(),
			"user":       contact.ContactUser,
		}
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"total":    len(contacts),
			"contacts": items,
		},
	})
}

func (h *ContactHandler) AddContact(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	
	var req struct {
		ContactID uint   `json:"contact_id" binding:"required"`
		Remark    string `json:"remark"`
		Tags      string `json:"tags"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request parameters",
			"data":    nil,
		})
		return
	}

	if userID == req.ContactID {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Cannot add yourself as contact",
			"data":    nil,
		})
		return
	}

	contact := &model.Contact{
		UserID:    userID,
		ContactID: req.ContactID,
		Remark:    req.Remark,
		Tags:      req.Tags,
		Status:    "normal",
	}

	if err := h.contactDAO.Create(contact); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Contact added successfully",
		"data":    contact,
	})
}

func (h *ContactHandler) DeleteContact(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	
	var req struct {
		ContactID uint `json:"contact_id" binding:"required"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request parameters",
			"data":    nil,
		})
		return
	}

	if err := h.contactDAO.Delete(userID, req.ContactID); err != nil {
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
		"data":    nil,
	})
}

