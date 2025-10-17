package middleware

import (
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
)

// FileValidator 文件验证中间件配置
type FileValidatorConfig struct {
	AllowedTypes []string // 允许的文件类型
	MaxSize      int64    // 最大文件大小（字节）
}

// FileTypeValidator 文件类型验证中间件
func FileTypeValidator(allowedTypes []string) gin.HandlerFunc {
	return func(c *gin.Context) {
		fileType := c.Query("file_type")
		
		if fileType == "" {
			c.Next()
			return
		}
		
		// 检查文件类型是否在白名单中
		allowed := false
		for _, t := range allowedTypes {
			if fileType == t || strings.HasPrefix(fileType, t+"/") {
				allowed = true
				break
			}
		}
		
		if !allowed {
			c.JSON(http.StatusBadRequest, gin.H{
				"code":    400,
				"message": "File type not allowed: " + fileType,
				"data":    nil,
			})
			c.Abort()
			return
		}
		
		c.Next()
	}
}

// FileSizeValidator 文件大小验证
// 注意：实际文件大小验证需要在上传时检查Content-Length
func FileSizeValidator(maxSize int64) gin.HandlerFunc {
	return func(c *gin.Context) {
		// 检查Content-Length header
		contentLength := c.Request.ContentLength
		if contentLength > maxSize {
			c.JSON(http.StatusBadRequest, gin.H{
				"code":    400,
				"message": "File size exceeds maximum allowed size",
				"data": gin.H{
					"max_size":     maxSize,
					"current_size": contentLength,
				},
			})
			c.Abort()
			return
		}
		
		c.Next()
	}
}

// ValidateFileType 验证文件类型（辅助函数）
func ValidateFileType(fileType string) bool {
	allowedTypes := []string{
		"image/jpeg",
		"image/jpg",
		"image/png",
		"image/gif",
		"image/webp",
		"video/mp4",
		"video/mpeg",
		"video/quicktime",
		"audio/mpeg",
		"audio/mp3",
		"audio/wav",
		"application/pdf",
		"application/msword",
		"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
		"application/vnd.ms-excel",
		"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
		"application/zip",
	}
	
	for _, allowed := range allowedTypes {
		if fileType == allowed {
			return true
		}
	}
	
	return false
}

// GetMaxFileSize 根据文件类型返回最大允许大小
func GetMaxFileSize(fileType string) int64 {
	switch {
	case strings.HasPrefix(fileType, "image/"):
		return 10 * 1024 * 1024 // 10MB
	case strings.HasPrefix(fileType, "video/"):
		return 100 * 1024 * 1024 // 100MB
	case strings.HasPrefix(fileType, "audio/"):
		return 20 * 1024 * 1024 // 20MB
	default:
		return 50 * 1024 * 1024 // 50MB
	}
}

