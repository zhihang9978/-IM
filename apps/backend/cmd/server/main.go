package main

import (
	"fmt"
	"log"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/config"
	"github.com/lanxin/im-backend/internal/middleware"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"github.com/lanxin/im-backend/internal/pkg/redis"
)

func main() {
	// 加载配置
	cfg := config.Load()

	// 设置Gin模式
	if cfg.Server.Mode == "release" {
		gin.SetMode(gin.ReleaseMode)
	}

	// 初始化数据库
	mysql.Init(cfg.Database.MySQL)
	defer mysql.Close()

	// 初始化Redis
	redis.Init(cfg.Redis)
	defer redis.Close()

	// 创建路由
	router := setupRouter(cfg)

	// 启动服务器
	addr := fmt.Sprintf(":%d", cfg.Server.Port)
	log.Printf("Server starting on %s", addr)
	log.Printf("Server mode: %s", cfg.Server.Mode)
	log.Printf("Domain: %s", cfg.Server.Domain)
	
	if err := router.Run(addr); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}

func setupRouter(cfg *config.Config) *gin.Engine {
	r := gin.New()

	// 全局中间件
	r.Use(gin.Recovery())
	r.Use(middleware.Logger())
	r.Use(middleware.CORS(cfg.Security.CORS))
	
	if cfg.Security.RateLimit.Enabled {
		r.Use(middleware.RateLimit(cfg.Security.RateLimit.RequestsPerMinute))
	}

	// 健康检查
	r.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"status": "ok",
			"message": "LanXin IM Server is running",
		})
	})

	// API路由组
	api := r.Group("/api/v1")
	{
		// 公开API
		public := api.Group("")
		{
			public.GET("/ping", func(c *gin.Context) {
				c.JSON(200, gin.H{
					"message": "pong",
				})
			})
			
			// TODO: 添加认证相关API
			// public.POST("/auth/login", authHandler.Login)
			// public.POST("/auth/register", authHandler.Register)
		}

		// 需要认证的API
		authorized := api.Group("")
		authorized.Use(middleware.JWTAuth(cfg.JWT.Secret))
		{
			// TODO: 添加需要认证的API
			// authorized.GET("/users/me", userHandler.GetProfile)
			// authorized.GET("/messages", messageHandler.GetMessages)
		}

		// 管理员API
		admin := api.Group("/admin")
		admin.Use(middleware.JWTAuth(cfg.JWT.Secret))
		admin.Use(middleware.AdminAuth())
		{
			// TODO: 添加管理员API
			// admin.GET("/users", adminHandler.GetUsers)
			// admin.POST("/users/:id/ban", adminHandler.BanUser)
		}
	}

	// WebSocket路由
	// TODO: 实现WebSocket服务
	// r.GET("/ws", func(c *gin.Context) {
	//     websocket.ServeWS(hub, c)
	// })

	return r
}

