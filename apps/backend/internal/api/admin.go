package api

import (
	"encoding/csv"
	"fmt"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/model"
	"golang.org/x/crypto/bcrypt"
)

type AdminHandler struct {
	userDAO *dao.UserDAO
}

func NewAdminHandler() *AdminHandler {
	return &AdminHandler{
		userDAO: dao.NewUserDAO(),
	}
}

func (h *AdminHandler) GetUsers(c *gin.Context) {
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))
	
	filters := make(map[string]interface{})
	if status := c.Query("status"); status != "" {
		filters["status"] = status
	}
	if role := c.Query("role"); role != "" {
		filters["role"] = role
	}
	
	users, total, err := h.userDAO.List(page, pageSize, filters)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to query users",
			"data":    nil,
		})
		return
	}
	
	userResponses := make([]interface{}, len(users))
	for i, user := range users {
		userResponses[i] = user.ToResponse()
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"users": userResponses,
			"total": total,
			"page":  page,
			"page_size": pageSize,
		},
	})
}

func (h *AdminHandler) CreateUser(c *gin.Context) {
	var req struct {
		Username string `json:"username" binding:"required"`
		Password string `json:"password" binding:"required"`
		Phone    string `json:"phone"`
		Email    string `json:"email"`
		Role     string `json:"role"`
	}
	
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}
	
	if req.Role == "" {
		req.Role = "user"
	}
	
	// 加密密码
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), 10)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to hash password",
			"data":    nil,
		})
		return
	}
	
	lanxinID := generateLanxinID()
	
	user := &model.User{
		Username: req.Username,
		Password: string(hashedPassword),
		Phone:    req.Phone,
		Email:    req.Email,
		LanxinID: lanxinID,
		Role:     req.Role,
		Status:   "active",
	}
	
	if err := h.userDAO.Create(user); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to create user",
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"user": user.ToResponse(),
		},
	})
}

func (h *AdminHandler) UpdateUser(c *gin.Context) {
	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid user ID",
			"data":    nil,
		})
		return
	}
	
	user, err := h.userDAO.GetByID(uint(id))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "User not found",
			"data":    nil,
		})
		return
	}
	
	var updates struct {
		Username *string `json:"username"`
		Phone    *string `json:"phone"`
		Email    *string `json:"email"`
		Avatar   *string `json:"avatar"`
		Role     *string `json:"role"`
		Status   *string `json:"status"`
	}
	
	if err := c.ShouldBindJSON(&updates); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}
	
	if updates.Username != nil {
		user.Username = *updates.Username
	}
	if updates.Phone != nil {
		user.Phone = *updates.Phone
	}
	if updates.Email != nil {
		user.Email = *updates.Email
	}
	if updates.Avatar != nil {
		user.Avatar = *updates.Avatar
	}
	if updates.Role != nil {
		user.Role = *updates.Role
	}
	if updates.Status != nil {
		user.Status = *updates.Status
	}
	
	if err := h.userDAO.Update(user); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to update user",
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"user": user.ToResponse(),
		},
	})
}

func (h *AdminHandler) DeleteUser(c *gin.Context) {
	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid user ID",
			"data":    nil,
		})
		return
	}
	
	if err := h.userDAO.Delete(uint(id)); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to delete user",
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

func (h *AdminHandler) BanUser(c *gin.Context) {
	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid user ID",
			"data":    nil,
		})
		return
	}
	
	user, err := h.userDAO.GetByID(uint(id))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "User not found",
			"data":    nil,
		})
		return
	}
	
	user.Status = "banned"
	
	if err := h.userDAO.Update(user); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to ban user",
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

func (h *AdminHandler) UnbanUser(c *gin.Context) {
	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid user ID",
			"data":    nil,
		})
		return
	}
	
	user, err := h.userDAO.GetByID(uint(id))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "User not found",
			"data":    nil,
		})
		return
	}
	
	user.Status = "active"
	
	if err := h.userDAO.Update(user); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to unban user",
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

func (h *AdminHandler) ExportUsers(c *gin.Context) {
	filters := make(map[string]interface{})
	if status := c.Query("status"); status != "" {
		filters["status"] = status
	}
	if role := c.Query("role"); role != "" {
		filters["role"] = role
	}
	
	users, _, err := h.userDAO.List(1, 10000, filters)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to query users for export",
			"data":    nil,
		})
		return
	}
	
	c.Header("Content-Type", "text/csv; charset=utf-8")
	c.Header("Content-Disposition", "attachment; filename=users_export.csv")
	
	writer := csv.NewWriter(c.Writer)
	defer writer.Flush()
	
	c.Writer.Write([]byte{0xEF, 0xBB, 0xBF})
	
	headers := []string{"ID", "用户名", "蓝信号", "手机号", "邮箱", "角色", "状态", "最后登录时间", "创建时间"}
	if err := writer.Write(headers); err != nil {
		return
	}
	
	for _, user := range users {
		lastLoginAt := ""
		if user.LastLoginAt != nil {
			lastLoginAt = user.LastLoginAt.Format("2006-01-02 15:04:05")
		}
		
		record := []string{
			fmt.Sprintf("%d", user.ID),
			user.Username,
			user.LanxinID,
			user.Phone,
			user.Email,
			user.Role,
			user.Status,
			lastLoginAt,
			user.CreatedAt.Format("2006-01-02 15:04:05"),
		}
		if err := writer.Write(record); err != nil {
			return
		}
	}
}

func (h *AdminHandler) ResetAdminPassword(c *gin.Context) {
	var req struct {
		Username string `json:"username" binding:"required"`
		Password string `json:"password" binding:"required"`
		SecretKey string `json:"secret_key" binding:"required"`
	}
	
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}
	
	if req.SecretKey != "reset-admin-2025" {
		c.JSON(http.StatusForbidden, gin.H{
			"code":    403,
			"message": "Invalid secret key",
			"data":    nil,
		})
		return
	}
	
	user, err := h.userDAO.GetByUsername(req.Username)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "User not found",
			"data":    nil,
		})
		return
	}
	
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), 10)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to hash password",
			"data":    nil,
		})
		return
	}
	
	user.Password = string(hashedPassword)
	user.Role = "admin"
	
	if err := h.userDAO.Update(user); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to update password",
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Password reset successfully",
		"data":    nil,
	})
}

func generateLanxinID() string {
	timestamp := time.Now().Unix()
	return "LX" + fmt.Sprintf("%d", timestamp)
}
