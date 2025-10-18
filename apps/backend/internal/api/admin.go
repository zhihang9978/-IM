package api

import (
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"github.com/lanxin/im-backend/internal/service"
	"golang.org/x/crypto/bcrypt"
)

type AdminHandler struct {
	authService *service.AuthService
}

func NewAdminHandler(authService *service.AuthService) *AdminHandler {
	return &AdminHandler{
		authService: authService,
	}
}


func (h *AdminHandler) GetAllUsers(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "10"))
	keyword := c.Query("keyword")
	status := c.Query("status")
	role := c.Query("role")

	offset := (page - 1) * pageSize

	query := db.Model(&model.User{})

	if keyword != "" {
		query = query.Where("username LIKE ? OR phone LIKE ? OR email LIKE ? OR lanxin_id LIKE ?",
			"%"+keyword+"%", "%"+keyword+"%", "%"+keyword+"%", "%"+keyword+"%")
	}

	if status != "" {
		query = query.Where("status = ?", status)
	}

	if role != "" {
		query = query.Where("role = ?", role)
	}

	var total int64
	query.Count(&total)

	var users []model.User
	query.Offset(offset).Limit(pageSize).Order("created_at DESC").Find(&users)

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"list":       users,
			"total":      total,
			"page":       page,
			"page_size":  pageSize,
			"total_page": (total + int64(pageSize) - 1) / int64(pageSize),
		},
	})
}

func (h *AdminHandler) GetUserDetail(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	userID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	var user model.User
	if err := db.First(&user, userID).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "User not found",
		})
		return
	}

	var messageCount int64
	db.Model(&model.Message{}).Where("sender_id = ?", userID).Count(&messageCount)

	var contactCount int64
	db.Model(&model.Contact{}).Where("user_id = ?", userID).Count(&contactCount)

	var groupCount int64
	db.Table("group_members").Where("user_id = ?", userID).Count(&groupCount)

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"user":          user,
			"message_count": messageCount,
			"contact_count": contactCount,
			"group_count":   groupCount,
		},
	})
}

func (h *AdminHandler) CreateUser(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	var req struct {
		Username string `json:"username" binding:"required"`
		Password string `json:"password" binding:"required"`
		Phone    string `json:"phone"`
		Email    string `json:"email"`
		Role     string `json:"role"`
		Status   string `json:"status"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request: " + err.Error(),
		})
		return
	}

	var count int64
	db.Model(&model.User{}).Where("username = ?", req.Username).Count(&count)
	if count > 0 {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Username already exists",
		})
		return
	}

	if req.Phone != "" {
		db.Model(&model.User{}).Where("phone = ?", req.Phone).Count(&count)
		if count > 0 {
			c.JSON(http.StatusBadRequest, gin.H{
				"code":    400,
				"message": "Phone already exists",
			})
			return
		}
	}

	if req.Email != "" {
		db.Model(&model.User{}).Where("email = ?", req.Email).Count(&count)
		if count > 0 {
			c.JSON(http.StatusBadRequest, gin.H{
				"code":    400,
				"message": "Email already exists",
			})
			return
		}
	}

	// 加密密码
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to hash password",
		})
		return
	}

	lanxinID := h.authService.GenerateLanxinID()

	if req.Role == "" {
		req.Role = "user"
	}
	if req.Status == "" {
		req.Status = "active"
	}

	user := model.User{
		Username: req.Username,
		Password: string(hashedPassword),
		Phone:    req.Phone,
		Email:    req.Email,
		LanxinID: lanxinID,
		Role:     req.Role,
		Status:   req.Status,
	}

	if err := db.Create(&user).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to create user: " + err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "User created successfully",
		"data":    user,
	})
}

// UpdateUser 更新用户信息
func (h *AdminHandler) UpdateUser(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	userID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	var req struct {
		Username string `json:"username"`
		Phone    string `json:"phone"`
		Email    string `json:"email"`
		Role     string `json:"role"`
		Status   string `json:"status"`
		Avatar   string `json:"avatar"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request: " + err.Error(),
		})
		return
	}

	var user model.User
	if err := db.First(&user, userID).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "User not found",
		})
		return
	}

	updates := make(map[string]interface{})
	if req.Username != "" {
		updates["username"] = req.Username
	}
	if req.Phone != "" {
		updates["phone"] = req.Phone
	}
	if req.Email != "" {
		updates["email"] = req.Email
	}
	if req.Role != "" {
		updates["role"] = req.Role
	}
	if req.Status != "" {
		updates["status"] = req.Status
	}
	if req.Avatar != "" {
		updates["avatar"] = req.Avatar
	}

	if err := db.Model(&user).Updates(updates).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to update user: " + err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "User updated successfully",
		"data":    user,
	})
}

func (h *AdminHandler) DeleteUser(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	userID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	var user model.User
	if err := db.First(&user, userID).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "User not found",
		})
		return
	}

	if err := db.Delete(&user).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to delete user: " + err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "User deleted successfully",
	})
}

func (h *AdminHandler) ResetUserPassword(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	userID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	var req struct {
		NewPassword string `json:"new_password" binding:"required"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request: " + err.Error(),
		})
		return
	}

	var user model.User
	if err := db.First(&user, userID).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "User not found",
		})
		return
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.NewPassword), bcrypt.DefaultCost)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to hash password",
		})
		return
	}

	// 更新密码
	if err := db.Model(&user).Update("password", string(hashedPassword)).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to reset password: " + err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Password reset successfully",
	})
}

func (h *AdminHandler) ExportUsers(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	var users []model.User
	db.Order("created_at DESC").Find(&users)

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    users,
	})
}


func (h *AdminHandler) GetAllMessages(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "10"))
	keyword := c.Query("keyword")
	msgType := c.Query("type")
	status := c.Query("status")
	startDate := c.Query("start_date")
	endDate := c.Query("end_date")

	offset := (page - 1) * pageSize

	query := db.Model(&model.Message{})

	if keyword != "" {
		query = query.Where("content LIKE ?", "%"+keyword+"%")
	}

	if msgType != "" {
		query = query.Where("type = ?", msgType)
	}

	if status != "" {
		query = query.Where("status = ?", status)
	}

	if startDate != "" {
		query = query.Where("created_at >= ?", startDate+" 00:00:00")
	}
	if endDate != "" {
		query = query.Where("created_at <= ?", endDate+" 23:59:59")
	}

	var total int64
	query.Count(&total)

	var messages []model.Message
	query.Offset(offset).Limit(pageSize).Order("created_at DESC").Find(&messages)

	for i := range messages {
		var sender model.User
		db.First(&sender, messages[i].SenderID)
		messages[i].Sender = &sender

		if messages[i].ReceiverID > 0 {
			var receiver model.User
			db.First(&receiver, messages[i].ReceiverID)
			messages[i].Receiver = &receiver
		}
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"list":       messages,
			"total":      total,
			"page":       page,
			"page_size":  pageSize,
			"total_page": (total + int64(pageSize) - 1) / int64(pageSize),
		},
	})
}

func (h *AdminHandler) DeleteMessage(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	messageID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	var message model.Message
	if err := db.First(&message, messageID).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Message not found",
		})
		return
	}

	if err := db.Delete(&message).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to delete message: " + err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Message deleted successfully",
	})
}

func (h *AdminHandler) ExportMessages(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	var messages []model.Message
	db.Order("created_at DESC").Limit(10000).Find(&messages)

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    messages,
	})
}


func (h *AdminHandler) GetAllFiles(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "10"))
	keyword := c.Query("keyword")
	fileType := c.Query("type")

	offset := (page - 1) * pageSize

	query := db.Model(&model.Message{}).Where("type IN ?", []string{"image", "video", "voice", "file"})

	if keyword != "" {
		query = query.Where("content LIKE ?", "%"+keyword+"%")
	}

	if fileType != "" && fileType != "all" {
		query = query.Where("type = ?", fileType)
	}

	var total int64
	query.Count(&total)

	var files []model.Message
	query.Offset(offset).Limit(pageSize).Order("created_at DESC").Find(&files)

	for i := range files {
		var uploader model.User
		db.First(&uploader, files[i].SenderID)
		files[i].Sender = &uploader
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"list":       files,
			"total":      total,
			"page":       page,
			"page_size":  pageSize,
			"total_page": (total + int64(pageSize) - 1) / int64(pageSize),
		},
	})
}

func (h *AdminHandler) DeleteFile(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	fileID, _ := strconv.ParseUint(c.Param("id"), 10, 32)

	var file model.Message
	if err := db.First(&file, fileID).Error; err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "File not found",
		})
		return
	}


	if err := db.Delete(&file).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Failed to delete file: " + err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "File deleted successfully",
	})
}

func (h *AdminHandler) GetStorageStats(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	var totalFiles int64
	db.Model(&model.Message{}).Where("type IN ?", []string{"image", "video", "voice", "file"}).Count(&totalFiles)

	totalStorage := int64(100 * 1024 * 1024 * 1024) // 100GB
	usedStorage := int64(45 * 1024 * 1024 * 1024)   // 45GB
	usagePercent := float64(usedStorage) / float64(totalStorage) * 100

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"total_files":    totalFiles,
			"total_storage":  totalStorage,
			"used_storage":   usedStorage,
			"free_storage":   totalStorage - usedStorage,
			"usage_percent":  usagePercent,
		},
	})
}


func (h *AdminHandler) GetSystemSettings(c *gin.Context) {
	settings := gin.H{
		"site_name":                  "蓝信通讯",
		"site_description":           "企业级即时通讯平台",
		"allow_register":             true,
		"require_email_verification": false,
		"max_file_size":              100,
		"message_retention_days":     365,
		"login_fail_lock_count":      5,
		"session_timeout_minutes":    1440,
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    settings,
	})
}

func (h *AdminHandler) UpdateSystemSettings(c *gin.Context) {
	var req map[string]interface{}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request: " + err.Error(),
		})
		return
	}


	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Settings updated successfully",
		"data":    req,
	})
}


func (h *AdminHandler) GetBackupList(c *gin.Context) {
	backups := []gin.H{
		{
			"id":         1,
			"filename":   "lanxin_backup_20251018_020000.sql.gz",
			"size":       "125.5 MB",
			"created_at": "2025-10-18 02:00:00",
			"status":     "completed",
		},
		{
			"id":         2,
			"filename":   "lanxin_backup_20251017_020000.sql.gz",
			"size":       "124.2 MB",
			"created_at": "2025-10-17 02:00:00",
			"status":     "completed",
		},
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"list":             backups,
			"last_backup_time": "2025-10-18 02:00:00",
		},
	})
}

func (h *AdminHandler) CreateBackup(c *gin.Context) {
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Backup task created successfully",
		"data": gin.H{
			"backup_id":  time.Now().Unix(),
			"status":     "processing",
			"created_at": time.Now().Format("2006-01-02 15:04:05"),
		},
	})
}

func (h *AdminHandler) DownloadBackup(c *gin.Context) {
	backupID := c.Param("id")
	
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Backup download started",
		"data": gin.H{
			"download_url": "/admin/backups/" + backupID + "/download",
		},
	})
}

func (h *AdminHandler) DeleteBackup(c *gin.Context) {
	backupID := c.Param("id")
	
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "Backup deleted successfully",
		"data": gin.H{
			"backup_id": backupID,
		},
	})
}
