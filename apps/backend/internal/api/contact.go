package api

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/middleware"
	"github.com/lanxin/im-backend/internal/model"
)

type ContactHandler struct {
	contactDAO *dao.ContactDAO
	logDAO     *dao.OperationLogDAO
}

func NewContactHandler() *ContactHandler {
	return &ContactHandler{
		contactDAO: dao.NewContactDAO(),
		logDAO:     dao.NewOperationLogDAO(),
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

// AddContact 添加联系人
// POST /contacts
// Body: {"contact_id": 123, "remark": "张三", "tags": "朋友,同事"}
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

	// 验证：不能添加自己为联系人
	if req.ContactID == userID {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Cannot add yourself as contact",
			"data":    nil,
		})
		return
	}

	// 检查是否已存在
	if h.contactDAO.CheckExists(userID, req.ContactID) {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Contact already exists",
			"data":    nil,
		})
		return
	}

	// 创建联系人
	contact := &model.Contact{
		UserID:    userID,
		ContactID: req.ContactID,
		Remark:    req.Remark,
		Tags:      req.Tags,
		Status:    model.ContactStatusNormal,
	}

	if err := h.contactDAO.Create(contact); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to add contact",
			"data":    nil,
		})
		return
	}

	// 重新加载以获取关联数据
	contact, _ = h.contactDAO.GetByID(contact.ID, userID)
	
	// ✅ 记录操作日志
	h.logDAO.CreateLog(dao.LogRequest{
		Action: "contact_add",
		UserID: &userID,
		IP:     c.ClientIP(),
		UserAgent: c.GetHeader("User-Agent"),
		Details: map[string]interface{}{
			"contact_id": req.ContactID,
			"remark":     req.Remark,
			"tags":       req.Tags,
		},
		Result: model.ResultSuccess,
	})
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Contact added successfully",
		"data": gin.H{
			"contact": contact,
		},
	})
}

// DeleteContact 删除联系人
// DELETE /contacts/:id
func (h *ContactHandler) DeleteContact(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	contactID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid contact ID",
			"data":    nil,
		})
		return
	}

	// 验证联系人是否存在且属于当前用户
	_, err = h.contactDAO.GetByID(uint(contactID), userID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Contact not found",
			"data":    nil,
		})
		return
	}

	// 删除
	if err := h.contactDAO.Delete(uint(contactID), userID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to delete contact",
			"data":    nil,
		})
		return
	}
	
	// ✅ 记录操作日志
	h.logDAO.CreateLog(dao.LogRequest{
		Action: "contact_delete",
		UserID: &userID,
		IP:     c.ClientIP(),
		UserAgent: c.GetHeader("User-Agent"),
		Details: map[string]interface{}{
			"contact_id": contactID,
		},
		Result: model.ResultSuccess,
	})
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Contact deleted successfully",
		"data":    nil,
	})
}

// UpdateRemark 更新联系人备注和标签
// PUT /contacts/:id/remark
// Body: {"remark": "张三", "tags": "朋友,同事"}
func (h *ContactHandler) UpdateRemark(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	contactID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid contact ID",
			"data":    nil,
		})
		return
	}

	var req struct {
		Remark string `json:"remark"`
		Tags   string `json:"tags"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request parameters",
			"data":    nil,
		})
		return
	}

	// 验证联系人是否存在
	_, err = h.contactDAO.GetByID(uint(contactID), userID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Contact not found",
			"data":    nil,
		})
		return
	}

	// 更新
	if err := h.contactDAO.UpdateRemark(uint(contactID), userID, req.Remark, req.Tags); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to update remark",
			"data":    nil,
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Remark updated successfully",
		"data":    nil,
	})
}
