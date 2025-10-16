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
	cosClient, err := cos.NewClient(cos.Config{
		SecretID:  cfg.TencentCloud.COS.SecretID,
		SecretKey: cfg.TencentCloud.COS.SecretKey,
		Bucket:    cfg.TencentCloud.COS.Bucket,
		Region:    cfg.TencentCloud.COS.Region,
		BaseURL:   cfg.TencentCloud.COS.BaseURL,
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

