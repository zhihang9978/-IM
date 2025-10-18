package api

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/middleware"
	"github.com/lanxin/im-backend/internal/service"
	"github.com/lanxin/im-backend/internal/websocket"
)

type GroupHandler struct {
	groupService *service.GroupService
}

func NewGroupHandler(hub *websocket.Hub) *GroupHandler {
	return &GroupHandler{
		groupService: service.NewGroupService(hub),
	}
}

// GET /api/v1/groups
func (h *GroupHandler) GetUserGroups(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))

	groups, total, err := h.groupService.GetUserGroups(userID, page, pageSize)
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
			"total":  total,
			"page":   page,
			"page_size": pageSize,
			"groups": groups,
		},
	})
}

// CreateGroup 创建群组
// POST /api/v1/groups
func (h *GroupHandler) CreateGroup(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)

	var req struct {
		Name      string `json:"name" binding:"required"`
		Avatar    string `json:"avatar"`
		MemberIDs []uint `json:"member_ids" binding:"required"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request: " + err.Error(),
			"data":    nil,
		})
		return
	}

	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")

	group, err := h.groupService.CreateGroup(
		userID,
		req.Name,
		req.Avatar,
		req.MemberIDs,
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
			"group": group,
		},
	})
}

// GetGroupInfo 获取群组信息
// GET /api/v1/groups/:id
func (h *GroupHandler) GetGroupInfo(c *gin.Context) {
	groupID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid group ID",
			"data":    nil,
		})
		return
	}

	group, err := h.groupService.GetGroupInfo(uint(groupID))
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
		"data": gin.H{
			"group": group,
		},
	})
}

// GetGroupMembers 获取群成员列表
// GET /api/v1/groups/:id/members
func (h *GroupHandler) GetGroupMembers(c *gin.Context) {
	groupID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid group ID",
			"data":    nil,
		})
		return
	}

	members, err := h.groupService.GetMembers(uint(groupID))
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
			"members": members,
		},
	})
}

// AddMembers 添加群成员
// POST /api/v1/groups/:id/members
func (h *GroupHandler) AddMembers(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	groupID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid group ID",
			"data":    nil,
		})
		return
	}

	var req struct {
		MemberIDs []uint `json:"member_ids" binding:"required"`
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

	err = h.groupService.AddMembers(uint(groupID), userID, req.MemberIDs, ip, userAgent)
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
		"data":    nil,
	})
}

// RemoveMember 移除群成员
// DELETE /api/v1/groups/:id/members/:user_id
func (h *GroupHandler) RemoveMember(c *gin.Context) {
	operatorID, _ := middleware.GetUserID(c)
	groupID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid group ID",
			"data":    nil,
		})
		return
	}

	memberID, err := strconv.ParseUint(c.Param("user_id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid user ID",
			"data":    nil,
		})
		return
	}

	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")

	err = h.groupService.RemoveMember(uint(groupID), operatorID, uint(memberID), ip, userAgent)
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
		"data":    nil,
	})
}

// SendGroupMessage 发送群消息
// POST /api/v1/groups/:id/messages
func (h *GroupHandler) SendGroupMessage(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	groupID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid group ID",
			"data":    nil,
		})
		return
	}

	var req struct {
		Content  string  `json:"content" binding:"required"`
		Type     string  `json:"type"`
		FileURL  *string `json:"file_url"`
		FileSize *int64  `json:"file_size"`
		Duration *int    `json:"duration"`
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

	message, err := h.groupService.SendGroupMessage(
		uint(groupID),
		userID,
		req.Content,
		req.Type,
		req.FileURL,
		req.FileSize,
		req.Duration,
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

// UpdateGroup 更新群组信息
// PUT /api/v1/groups/:id
func (h *GroupHandler) UpdateGroup(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	groupID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid group ID",
			"data":    nil,
		})
		return
	}

	var req struct {
		Name   string `json:"name"`
		Avatar string `json:"avatar"`
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

	err = h.groupService.UpdateGroup(uint(groupID), userID, req.Name, req.Avatar, ip, userAgent)
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
		"data":    nil,
	})
}

// DisbandGroup 解散群组
// DELETE /api/v1/groups/:id
func (h *GroupHandler) DisbandGroup(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	groupID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid group ID",
			"data":    nil,
		})
		return
	}

	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")

	err = h.groupService.DisbandGroup(uint(groupID), userID, ip, userAgent)
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
		"data":    nil,
	})
}

