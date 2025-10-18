package api

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/middleware"
	"github.com/lanxin/im-backend/internal/model"
)

type FriendRequestHandler struct {
	friendRequestDAO *dao.FriendRequestDAO
	contactDAO       *dao.ContactDAO
	userDAO          *dao.UserDAO
	logDAO           *dao.OperationLogDAO
}

func NewFriendRequestHandler() *FriendRequestHandler {
	return &FriendRequestHandler{
		friendRequestDAO: dao.NewFriendRequestDAO(),
		contactDAO:       dao.NewContactDAO(),
		userDAO:          dao.NewUserDAO(),
		logDAO:           dao.NewOperationLogDAO(),
	}
}

func (h *FriendRequestHandler) SendFriendRequest(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)

	var req struct {
		ReceiverID uint   `json:"receiver_id" binding:"required"`
		Message    string `json:"message"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request parameters",
			"data":    nil,
		})
		return
	}

	if req.ReceiverID == userID {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Cannot send friend request to yourself",
			"data":    nil,
		})
		return
	}

	if _, err := h.userDAO.GetByID(req.ReceiverID); err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "User not found",
			"data":    nil,
		})
		return
	}

	if h.contactDAO.CheckExists(userID, req.ReceiverID) {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Already friends",
			"data":    nil,
		})
		return
	}

	if h.friendRequestDAO.CheckExists(userID, req.ReceiverID) {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Friend request already sent",
			"data":    nil,
		})
		return
	}

	friendRequest := &model.FriendRequest{
		SenderID:   userID,
		ReceiverID: req.ReceiverID,
		Message:    req.Message,
		Status:     model.FriendRequestStatusPending,
	}

	if err := h.friendRequestDAO.Create(friendRequest); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to send friend request",
			"data":    nil,
		})
		return
	}

	friendRequest, _ = h.friendRequestDAO.GetByID(friendRequest.ID)

	h.logDAO.CreateLog(dao.LogRequest{
		Action:    "friend_request_send",
		UserID:    &userID,
		IP:        c.ClientIP(),
		UserAgent: c.GetHeader("User-Agent"),
		Details: map[string]interface{}{
			"receiver_id": req.ReceiverID,
			"message":     req.Message,
		},
		Result: model.ResultSuccess,
	})

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Friend request sent successfully",
		"data": gin.H{
			"friend_request": friendRequest,
		},
	})
}

func (h *FriendRequestHandler) GetFriendRequests(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))
	requestType := c.DefaultQuery("type", "received")

	var requests []model.FriendRequest
	var total int64
	var err error

	if requestType == "sent" {
		requests, total, err = h.friendRequestDAO.GetSentRequests(userID, page, pageSize)
	} else {
		requests, total, err = h.friendRequestDAO.GetReceivedRequests(userID, page, pageSize)
	}

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
			"page":     page,
			"page_size": pageSize,
			"requests": requests,
		},
	})
}

func (h *FriendRequestHandler) AcceptFriendRequest(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	requestID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request ID",
			"data":    nil,
		})
		return
	}

	friendRequest, err := h.friendRequestDAO.GetByID(uint(requestID))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Friend request not found",
			"data":    nil,
		})
		return
	}

	if friendRequest.ReceiverID != userID {
		c.JSON(http.StatusForbidden, gin.H{
			"code":    403,
			"message": "No permission",
			"data":    nil,
		})
		return
	}

	if friendRequest.Status != model.FriendRequestStatusPending {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Request already processed",
			"data":    nil,
		})
		return
	}

	if err := h.friendRequestDAO.UpdateStatus(uint(requestID), model.FriendRequestStatusAccepted); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to update request status",
			"data":    nil,
		})
		return
	}

	contact1 := &model.Contact{
		UserID:    friendRequest.ReceiverID,
		ContactID: friendRequest.SenderID,
		Status:    model.ContactStatusNormal,
	}

	contact2 := &model.Contact{
		UserID:    friendRequest.SenderID,
		ContactID: friendRequest.ReceiverID,
		Status:    model.ContactStatusNormal,
	}

	if err := h.contactDAO.Create(contact1); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to add contact",
			"data":    nil,
		})
		return
	}

	if err := h.contactDAO.Create(contact2); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to add contact",
			"data":    nil,
		})
		return
	}

	h.logDAO.CreateLog(dao.LogRequest{
		Action:    "friend_request_accept",
		UserID:    &userID,
		IP:        c.ClientIP(),
		UserAgent: c.GetHeader("User-Agent"),
		Details: map[string]interface{}{
			"request_id": requestID,
			"sender_id":  friendRequest.SenderID,
		},
		Result: model.ResultSuccess,
	})

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Friend request accepted",
		"data":    nil,
	})
}

func (h *FriendRequestHandler) RejectFriendRequest(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	requestID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request ID",
			"data":    nil,
		})
		return
	}

	friendRequest, err := h.friendRequestDAO.GetByID(uint(requestID))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Friend request not found",
			"data":    nil,
		})
		return
	}

	if friendRequest.ReceiverID != userID {
		c.JSON(http.StatusForbidden, gin.H{
			"code":    403,
			"message": "No permission",
			"data":    nil,
		})
		return
	}

	if friendRequest.Status != model.FriendRequestStatusPending {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Request already processed",
			"data":    nil,
		})
		return
	}

	if err := h.friendRequestDAO.UpdateStatus(uint(requestID), model.FriendRequestStatusRejected); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to update request status",
			"data":    nil,
		})
		return
	}

	h.logDAO.CreateLog(dao.LogRequest{
		Action:    "friend_request_reject",
		UserID:    &userID,
		IP:        c.ClientIP(),
		UserAgent: c.GetHeader("User-Agent"),
		Details: map[string]interface{}{
			"request_id": requestID,
			"sender_id":  friendRequest.SenderID,
		},
		Result: model.ResultSuccess,
	})

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Friend request rejected",
		"data":    nil,
	})
}
