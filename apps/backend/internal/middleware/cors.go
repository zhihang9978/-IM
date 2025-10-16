package middleware

import (
	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/config"
)

// CORS 跨域中间件
func CORS(cfg config.CORSConfig) gin.HandlerFunc {
	return func(c *gin.Context) {
		origin := c.Request.Header.Get("Origin")
		
		// 检查origin是否在允许列表中
		allowed := false
		for _, allowedOrigin := range cfg.AllowedOrigins {
			if origin == allowedOrigin || allowedOrigin == "*" {
				allowed = true
				break
			}
		}

		if allowed {
			c.Writer.Header().Set("Access-Control-Allow-Origin", origin)
		}

		c.Writer.Header().Set("Access-Control-Allow-Credentials", "true")
		
		// 设置允许的方法
		methods := "GET, POST, PUT, DELETE, OPTIONS"
		if len(cfg.AllowedMethods) > 0 {
			methods = ""
			for i, method := range cfg.AllowedMethods {
				if i > 0 {
					methods += ", "
				}
				methods += method
			}
		}
		c.Writer.Header().Set("Access-Control-Allow-Methods", methods)

		// 设置允许的头部
		headers := "Authorization, Content-Type"
		if len(cfg.AllowedHeaders) > 0 {
			headers = ""
			for i, header := range cfg.AllowedHeaders {
				if i > 0 {
					headers += ", "
				}
				headers += header
			}
		}
		c.Writer.Header().Set("Access-Control-Allow-Headers", headers)

		c.Writer.Header().Set("Access-Control-Max-Age", "86400")

		// 处理OPTIONS请求
		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}

		c.Next()
	}
}

