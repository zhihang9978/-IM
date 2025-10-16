package api

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/middleware"
	"github.com/lanxin/im-backend/internal/service"
)

type GroupHandler struct {
	groupService *service.GroupService
}

func NewGroupHandler() *GroupHandler {
	return &GroupHandler{
		groupService: service.NewGroupService(),
	}
}

// CreateGroup 创建群组
func (h *GroupHandler) CreateGroup(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)

	var req struct {
		Name        string   `json:"name" binding:"required"`
		Description string   `json:"description"`
		MemberIDs   []uint   `json:"member_ids"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}

	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")

	group, err := h.groupService.CreateGroup(userID, req.Name, req.Description, req.MemberIDs, ip, userAgent)
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
			"group": group,
		},
	})
}

// GetGroup 获取群组信息
func (h *GroupHandler) GetGroup(c *gin.Context) {
	groupID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	group, err := h.groupService.GetGroup(uint(groupID))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Group not found",
			"data":    nil,
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    group,
	})
}

// GetUserGroups 获取用户的群组列表
func (h *GroupHandler) GetUserGroups(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)

	groups, err := h.groupService.GetUserGroups(userID)
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
			"groups": groups,
		},
	})
}

// UpdateGroup 更新群组信息
func (h *GroupHandler) UpdateGroup(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	groupID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	var req struct {
		Name        string `json:"name"`
		Description string `json:"description"`
		Avatar      string `json:"avatar"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}

	updates := make(map[string]interface{})
	if req.Name != "" {
		updates["name"] = req.Name
	}
	if req.Description != "" {
		updates["description"] = req.Description
	}
	if req.Avatar != "" {
		updates["avatar"] = req.Avatar
	}

	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")

	if err := h.groupService.UpdateGroup(uint(groupID), userID, updates, ip, userAgent); err != nil {
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

// AddMember 添加群成员
func (h *GroupHandler) AddMember(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	groupID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	var req struct {
		UserID uint `json:"user_id" binding:"required"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}

	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")

	if err := h.groupService.AddMember(uint(groupID), userID, req.UserID, ip, userAgent); err != nil {
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

// RemoveMember 移除群成员
func (h *GroupHandler) RemoveMember(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	groupID, _ := strconv.ParseUint(c.Param("id"), 10, 32)
	memberID, _ := strconv.ParseUint(c.Param("member_id"), 10, 32)

	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")

	if err := h.groupService.RemoveMember(uint(groupID), userID, uint(memberID), ip, userAgent); err != nil {
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

// GetMembers 获取群成员列表
func (h *GroupHandler) GetMembers(c *gin.Context) {
	groupID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	members, err := h.groupService.GetMembers(uint(groupID))
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
			"members": members,
		},
	})
}

// DisbandGroup 解散群组（管理员接口）
func (h *GroupHandler) DisbandGroup(c *gin.Context) {
	adminID, _ := middleware.GetUserID(c)
	groupID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	var req struct {
		Reason string `json:"reason"`
	}
	c.ShouldBindJSON(&req)

	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")

	if err := h.groupService.DisbandGroup(uint(groupID), adminID, req.Reason, ip, userAgent); err != nil {
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

