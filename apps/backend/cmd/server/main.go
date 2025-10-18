package main

import (
	"fmt"
	"log"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/config"
	"github.com/lanxin/im-backend/internal/api"
	"github.com/lanxin/im-backend/internal/middleware"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"github.com/lanxin/im-backend/internal/pkg/redis"
	"github.com/lanxin/im-backend/internal/service"
	"github.com/lanxin/im-backend/internal/websocket"
	"github.com/lanxin/im-backend/pkg/kafka"
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

	// 初始化Kafka Producer
	producer := kafka.NewProducer(cfg.Kafka.Brokers, cfg.Kafka.Topic.Message)
	defer producer.Close()

	// 初始化WebSocket Hub
	hub := websocket.NewHub()
	go hub.Run()

	// 创建路由
	router := setupRouter(cfg, hub, producer)

	// 启动服务器
	addr := fmt.Sprintf(":%d", cfg.Server.Port)
	log.Printf("Server starting on %s", addr)
	log.Printf("Server mode: %s", cfg.Server.Mode)
	log.Printf("Domain: %s", cfg.Server.Domain)
	log.Printf("WebSocket Hub started")

	if err := router.Run(addr); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}

func setupRouter(cfg *config.Config, hub *websocket.Hub, producer *kafka.Producer) *gin.Engine {
	r := gin.New()

	// 全局中间件
	r.Use(gin.Recovery())
	r.Use(middleware.Logger())
	r.Use(middleware.CORS(cfg.Security.CORS))

	if cfg.Security.RateLimit.Enabled {
		r.Use(middleware.RateLimit(cfg.Security.RateLimit.RequestsPerMinute))
	}

	// 创建Handler
	authService := service.NewAuthService(cfg)
	authHandler := api.NewAuthHandler(cfg)
	userHandler := api.NewUserHandler()
	messageHandler := api.NewMessageHandler(hub, producer)
	fileHandler, _ := api.NewFileHandler(cfg)
	trtcHandler := api.NewTRTCHandler(cfg, hub)
	conversationHandler := api.NewConversationHandler()
	contactHandler := api.NewContactHandler()
	favoriteHandler := api.NewFavoriteHandler()
	reportHandler := api.NewReportHandler()
	groupHandler := api.NewGroupHandler(hub)
	adminHandler := api.NewAdminHandler(authService)

	// 健康检查
	r.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"status":       "ok",
			"message":      "LanXin IM Server is running",
			"online_users": hub.GetOnlineUserCount(),
		})
	})

	// WebSocket路由
	r.GET("/ws", func(c *gin.Context) {
		websocket.ServeWS(hub, c, cfg.JWT.Secret)
	})

	// API路由组
	apiV1 := r.Group("/api/v1")
	{
		// 公开API（不需要认证）
		public := apiV1.Group("")
		{
			public.GET("/ping", func(c *gin.Context) {
				c.JSON(200, gin.H{"message": "pong"})
			})

			// 认证相关
			public.POST("/auth/register", authHandler.Register)
			public.POST("/auth/login", authHandler.Login)
		}

		// 需要认证的API
		authorized := apiV1.Group("")
		authorized.Use(middleware.JWTAuth(cfg.JWT.Secret))
		{
			// 认证相关
			authorized.POST("/auth/refresh", authHandler.RefreshToken)
			authorized.POST("/auth/logout", authHandler.Logout)

			// 用户相关
			authorized.GET("/users/me", userHandler.GetCurrentUser)
			authorized.PUT("/users/me", userHandler.UpdateProfile)
			authorized.PUT("/users/me/password", userHandler.ChangePassword)
			authorized.GET("/users/search", userHandler.SearchUsers)

			// 会话相关（Android客户端需要）
			authorized.GET("/conversations", conversationHandler.GetConversations)
			authorized.GET("/conversations/:id/settings", conversationHandler.GetConversationSettings)
			authorized.PUT("/conversations/:id/settings", conversationHandler.UpdateConversationSettings)

			// 联系人相关（Android客户端需要）
			authorized.GET("/contacts", contactHandler.GetContacts)
			authorized.POST("/contacts", contactHandler.AddContact)
			authorized.DELETE("/contacts/:id", contactHandler.DeleteContact)
			authorized.PUT("/contacts/:id/remark", contactHandler.UpdateRemark)

			// 消息相关
			authorized.POST("/messages", messageHandler.SendMessage)
			authorized.POST("/messages/:id/recall", messageHandler.RecallMessage)
			authorized.GET("/conversations/:id/messages", messageHandler.GetMessages)
			authorized.GET("/conversations/:id/messages/history", messageHandler.GetHistoryMessages)
			authorized.GET("/messages/search", messageHandler.SearchMessages)
			authorized.GET("/messages/offline", messageHandler.GetOfflineMessages)
			authorized.POST("/conversations/:id/read", messageHandler.MarkAsRead)

			// 文件相关
			authorized.GET("/files/upload-token", fileHandler.GetUploadToken)
			authorized.POST("/files/upload-callback", fileHandler.UploadCallback)

			// 收藏相关
			authorized.POST("/messages/collect", favoriteHandler.CollectMessage)
			authorized.GET("/favorites", favoriteHandler.GetFavorites)
			authorized.DELETE("/favorites/:id", favoriteHandler.DeleteFavorite)

			// 举报相关
			authorized.POST("/messages/report", reportHandler.ReportMessage)
			authorized.GET("/reports", reportHandler.GetReports)

			// 群组相关
			authorized.POST("/groups", groupHandler.CreateGroup)
			authorized.GET("/groups/:id", groupHandler.GetGroupInfo)
			authorized.GET("/groups/:id/members", groupHandler.GetGroupMembers)
			authorized.POST("/groups/:id/members", groupHandler.AddMembers)
			authorized.DELETE("/groups/:id/members/:user_id", groupHandler.RemoveMember)
			authorized.POST("/groups/:id/messages", groupHandler.SendGroupMessage)
			authorized.PUT("/groups/:id", groupHandler.UpdateGroup)
			authorized.DELETE("/groups/:id", groupHandler.DisbandGroup)

			// TRTC相关（纯数据流接口）
			authorized.POST("/trtc/user-sig", trtcHandler.GetUserSig)
			authorized.POST("/trtc/call", trtcHandler.InitiateCall)
			authorized.POST("/trtc/call/end", trtcHandler.EndCall)
			authorized.POST("/trtc/screen-share/start", trtcHandler.StartScreenShare)
			authorized.POST("/trtc/screen-share/end", trtcHandler.EndScreenShare)
		}

	// 管理员API
	admin := apiV1.Group("/admin")
	admin.Use(middleware.JWTAuth(cfg.JWT.Secret))
	admin.Use(middleware.AdminAuth())
	{
		// 用户管理
		admin.GET("/users", adminHandler.GetAllUsers)
		admin.GET("/users/:id", adminHandler.GetUserDetail)
		admin.POST("/users", adminHandler.CreateUser)
		admin.PUT("/users/:id", adminHandler.UpdateUser)
		admin.DELETE("/users/:id", adminHandler.DeleteUser)
		admin.POST("/users/:id/reset-password", adminHandler.ResetUserPassword)
		admin.GET("/users/export", adminHandler.ExportUsers)

		admin.GET("/messages", adminHandler.GetAllMessages)
		admin.DELETE("/messages/:id", adminHandler.DeleteMessage)
		admin.GET("/messages/export", adminHandler.ExportMessages)

		admin.GET("/files", adminHandler.GetAllFiles)
		admin.DELETE("/files/:id", adminHandler.DeleteFile)
		admin.GET("/storage/stats", adminHandler.GetStorageStats)

		// 举报管理
		admin.GET("/reports", reportHandler.GetAllReports)
		admin.PUT("/reports/:id", reportHandler.UpdateReportStatus)

		systemMonitorHandler := api.NewSystemMonitorHandler(hub)
		admin.GET("/system/metrics", systemMonitorHandler.GetSystemMetrics)
		admin.GET("/system/services", systemMonitorHandler.GetServiceStatus)
		admin.GET("/system/runtime", systemMonitorHandler.GetGoRuntimeMetrics)
		admin.GET("/dashboard/stats", systemMonitorHandler.GetDashboardStats)
		admin.GET("/dashboard/user-growth", systemMonitorHandler.GetUserGrowthTrend)
		admin.GET("/dashboard/message-stats", systemMonitorHandler.GetMessageTypeStats)
		admin.GET("/dashboard/device-distribution", systemMonitorHandler.GetOnlineDeviceDistribution)
		admin.GET("/health-check", systemMonitorHandler.HealthCheck)

		admin.GET("/settings", adminHandler.GetSystemSettings)
		admin.PUT("/settings", adminHandler.UpdateSystemSettings)

		admin.GET("/backups", adminHandler.GetBackupList)
		admin.POST("/backups", adminHandler.CreateBackup)
		admin.GET("/backups/:id/download", adminHandler.DownloadBackup)
		admin.DELETE("/backups/:id", adminHandler.DeleteBackup)
	}
	}

	return r
}

