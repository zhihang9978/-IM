package api

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/config"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/middleware"
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/pkg/cos"
)

type FileHandler struct {
	cosClient *cos.Client
	logDAO    *dao.OperationLogDAO
}

func NewFileHandler(cfg *config.Config) (*FileHandler, error) {
	// 使用自建COS服务
	cosClient, err := cos.NewClient(cos.Config{
		SecretID:  cfg.Storage.COS.SecretID,
		SecretKey: cfg.Storage.COS.SecretKey,
		Bucket:    cfg.Storage.COS.Bucket,
		Region:    cfg.Storage.COS.Region,
		BaseURL:   cfg.Storage.COS.BaseURL,
	})
	if err != nil {
		return nil, err
	}

	return &FileHandler{
		cosClient: cosClient,
		logDAO:    dao.NewOperationLogDAO(),
	}, nil
}

// GetUploadToken 获取上传凭证（客户端直传COS）
func (h *FileHandler) GetUploadToken(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)

	fileType := c.Query("file_type")
	fileName := c.Query("file_name")

	if fileName == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "file_name required",
			"data":    nil,
		})
		return
	}
	
	// ✅ 验证文件类型
	if fileType != "" && !middleware.ValidateFileType(fileType) {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "File type not allowed: " + fileType,
			"data":    nil,
		})
		return
	}

	token, err := h.cosClient.GenerateUploadToken(fileName, fileType)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}

	// 记录操作日志
	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")
	h.logDAO.CreateLog(dao.LogRequest{
		Action:    "file_upload_token_generated",
		UserID:    &userID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"file_name": fileName,
			"file_type": fileType,
		},
		Result: model.ResultSuccess,
	})

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    token,
	})
}

// UploadCallback 上传完成回调
func (h *FileHandler) UploadCallback(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)

	var req struct {
		Key         string `json:"key" binding:"required"`
		URL         string `json:"url" binding:"required"`
		Size        int64  `json:"size"`
		ContentType string `json:"content_type"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}
	
	// ✅ 验证文件大小
	maxSize := middleware.GetMaxFileSize(req.ContentType)
	if req.Size > maxSize {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "File size exceeds maximum allowed",
			"data": gin.H{
				"max_size":     maxSize,
				"current_size": req.Size,
			},
		})
		return
	}

	// 记录上传完成日志
	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")
	h.logDAO.CreateLog(dao.LogRequest{
		Action:    model.ActionFileUpload,
		UserID:    &userID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"file_key":      req.Key,
			"file_size":     req.Size,
			"content_type":  req.ContentType,
			"file_url":      req.URL,
		},
		Result: model.ResultSuccess,
	})

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    nil,
	})
}

