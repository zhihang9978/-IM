package api

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/config"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/middleware"
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/service"
	"github.com/lanxin/im-backend/internal/websocket"
)

type TRTCHandler struct {
	trtcService *service.TRTCService
	logDAO      *dao.OperationLogDAO
	hub         *websocket.Hub
}

func NewTRTCHandler(cfg *config.Config, hub *websocket.Hub) *TRTCHandler {
	return &TRTCHandler{
		trtcService: service.NewTRTCService(cfg.TRTC.SDKAppID, cfg.TRTC.SecretKey),
		logDAO:      dao.NewOperationLogDAO(),
		hub:         hub,
	}
}

// GetUserSig 获取TRTC UserSig（纯数据流接口，不涉及UI）
func (h *TRTCHandler) GetUserSig(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)

	var req struct {
		RoomID string `json:"room_id" binding:"required"`
		UserID uint   `json:"user_id" binding:"required"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}

	// 验证请求的UserID与当前登录用户一致
	if req.UserID != userID {
		c.JSON(http.StatusForbidden, gin.H{
			"code":    403,
			"message": "Cannot get UserSig for other users",
			"data":    nil,
		})
		return
	}

	// 生成UserSig（纯数据凭证）
	credentials, err := h.trtcService.GetCallCredentials(userID, req.RoomID, 86400)
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
		"data":    credentials,
	})
}

// InitiateCall 发起通话（纯数据接口，不涉及UI）
func (h *TRTCHandler) InitiateCall(c *gin.Context) {
	callerID, _ := middleware.GetUserID(c)

	var req struct {
		ReceiverID uint   `json:"receiver_id" binding:"required"`
		CallType   string `json:"call_type" binding:"required"` // audio, video
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}

	// 生成房间ID
	roomID := h.trtcService.GenerateRoomID(callerID, req.ReceiverID)

	// 通过WebSocket通知接收者（纯数据推送）
	callInviteData := map[string]interface{}{
		"caller_id":       callerID,
		"caller_username": c.GetString("username"),
		"room_id":         roomID,
		"call_type":       req.CallType,
	}

	if h.hub.IsUserOnline(req.ReceiverID) {
		h.hub.SendCallInvite(req.ReceiverID, callInviteData)
	}

	// 记录操作日志
	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")
	h.logDAO.CreateLog(dao.LogRequest{
		Action:    model.ActionCallInitiated,
		UserID:    &callerID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"receiver_id": req.ReceiverID,
			"call_type":   req.CallType,
			"room_id":     roomID,
		},
		Result: model.ResultSuccess,
	})

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"room_id":   roomID,
			"call_type": req.CallType,
		},
	})
}

// EndCall 结束通话（记录日志）
func (h *TRTCHandler) EndCall(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)

	var req struct {
		RoomID   string `json:"room_id" binding:"required"`
		Duration int    `json:"duration"` // 通话时长（秒）
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}

	// 记录通话结束日志
	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")
	h.logDAO.CreateLog(dao.LogRequest{
		Action:    model.ActionCallEnded,
		UserID:    &userID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"room_id":  req.RoomID,
			"duration": req.Duration,
		},
		Result: model.ResultSuccess,
	})

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    nil,
	})
}

// StartScreenShare 开始屏幕共享（记录日志，不涉及UI）
func (h *TRTCHandler) StartScreenShare(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)

	var req struct {
		RoomID string `json:"room_id" binding:"required"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}

	// 记录屏幕共享开始日志
	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")
	h.logDAO.CreateLog(dao.LogRequest{
		Action:    model.ActionScreenShareStart,
		UserID:    &userID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"room_id": req.RoomID,
		},
		Result: model.ResultSuccess,
	})

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    nil,
	})
}

// EndScreenShare 结束屏幕共享（记录日志）
func (h *TRTCHandler) EndScreenShare(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)

	var req struct {
		RoomID   string `json:"room_id" binding:"required"`
		Duration int    `json:"duration"` // 共享时长（秒）
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}

	// 记录屏幕共享结束日志
	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")
	h.logDAO.CreateLog(dao.LogRequest{
		Action:    model.ActionScreenShareEnd,
		UserID:    &userID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"room_id":  req.RoomID,
			"duration": req.Duration,
		},
		Result: model.ResultSuccess,
	})

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    nil,
	})
}

