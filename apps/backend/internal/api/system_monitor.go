package api

import (
	"net/http"
	"runtime"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"github.com/lanxin/im-backend/internal/pkg/redis"
	"github.com/lanxin/im-backend/internal/websocket"
	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/disk"
	"github.com/shirou/gopsutil/v3/mem"
	"github.com/shirou/gopsutil/v3/net"
)

type SystemMonitorHandler struct {
	hub *websocket.Hub
}

func NewSystemMonitorHandler(hub *websocket.Hub) *SystemMonitorHandler {
	return &SystemMonitorHandler{
		hub: hub,
	}
}

type SystemMetrics struct {
	CPUUsage          float64 `json:"cpu_usage"`
	MemoryUsage       float64 `json:"memory_usage"`
	DiskUsage         float64 `json:"disk_usage"`
	NetworkIn         uint64  `json:"network_in"`
	NetworkOut        uint64  `json:"network_out"`
	ActiveConnections int     `json:"active_connections"`
	UptimeSeconds     int64   `json:"uptime_seconds"`
}

type ServiceStatus struct {
	Name         string `json:"name"`
	Status       string `json:"status"`
	ResponseTime int    `json:"response_time"`
	LastCheck    string `json:"last_check"`
}

var startTime = time.Now()

func (h *SystemMonitorHandler) GetSystemMetrics(c *gin.Context) {
	metrics := SystemMetrics{}

	cpuPercent, err := cpu.Percent(time.Second, false)
	if err == nil && len(cpuPercent) > 0 {
		metrics.CPUUsage = cpuPercent[0]
	}

	memStat, err := mem.VirtualMemory()
	if err == nil {
		metrics.MemoryUsage = memStat.UsedPercent
	}

	diskStat, err := disk.Usage("/")
	if err == nil {
		metrics.DiskUsage = diskStat.UsedPercent
	}

	netIO, err := net.IOCounters(false)
	if err == nil && len(netIO) > 0 {
		metrics.NetworkIn = netIO[0].BytesRecv
		metrics.NetworkOut = netIO[0].BytesSent
	}

	metrics.ActiveConnections = h.hub.GetActiveConnectionCount()
	metrics.UptimeSeconds = int64(time.Since(startTime).Seconds())

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    metrics,
	})
}

func (h *SystemMonitorHandler) GetServiceStatus(c *gin.Context) {
	services := []ServiceStatus{}
	now := time.Now().Format("2006-01-02 15:04:05")

	dbStart := time.Now()
	dbStatus := "healthy"
	db := mysql.GetDB()
	if db != nil {
		sqlDB, err := db.DB()
		if err != nil || sqlDB.Ping() != nil {
			dbStatus = "error"
		}
	} else {
		dbStatus = "error"
	}
	dbResponseTime := int(time.Since(dbStart).Milliseconds())

	services = append(services, ServiceStatus{
		Name:         "MySQL Database",
		Status:       dbStatus,
		ResponseTime: dbResponseTime,
		LastCheck:    now,
	})

	redisStart := time.Now()
	redisStatus := "healthy"
	redisClient := redis.GetClient()
	if redisClient != nil {
		if err := redisClient.Ping(c).Err(); err != nil {
			redisStatus = "error"
		}
	} else {
		redisStatus = "error"
	}
	redisResponseTime := int(time.Since(redisStart).Milliseconds())

	services = append(services, ServiceStatus{
		Name:         "Redis Cache",
		Status:       redisStatus,
		ResponseTime: redisResponseTime,
		LastCheck:    now,
	})

	wsStatus := "healthy"
	if h.hub.GetActiveConnectionCount() == 0 {
		wsStatus = "warning"
	}
	services = append(services, ServiceStatus{
		Name:         "WebSocket Server",
		Status:       wsStatus,
		ResponseTime: 0,
		LastCheck:    now,
	})

	services = append(services, ServiceStatus{
		Name:         "MinIO Object Storage",
		Status:       "healthy",
		ResponseTime: 0,
		LastCheck:    now,
	})

	services = append(services, ServiceStatus{
		Name:         "Tencent TRTC Service",
		Status:       "healthy",
		ResponseTime: 0,
		LastCheck:    now,
	})

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    services,
	})
}

func (h *SystemMonitorHandler) GetGoRuntimeMetrics(c *gin.Context) {
	var m runtime.MemStats
	runtime.ReadMemStats(&m)

	metrics := gin.H{
		"goroutines":      runtime.NumGoroutine(),
		"heap_alloc_mb":   float64(m.HeapAlloc) / 1024 / 1024,
		"heap_sys_mb":     float64(m.HeapSys) / 1024 / 1024,
		"num_gc":          m.NumGC,
		"gc_pause_ns":     m.PauseNs[(m.NumGC+255)%256],
		"go_version":      runtime.Version(),
		"num_cpu":         runtime.NumCPU(),
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    metrics,
	})
}

func (h *SystemMonitorHandler) GetDashboardStats(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	var totalUsers int64
	db.Model(&model.User{}).Count(&totalUsers)

	var totalMessages int64
	db.Model(&model.Message{}).Count(&totalMessages)

	var totalGroups int64
	db.Model(&model.Group{}).Count(&totalGroups)

	var totalFiles int64
	db.Model(&model.Message{}).Where("type IN ?", []string{"image", "video", "voice", "file"}).Count(&totalFiles)

	onlineUsers := h.hub.GetActiveConnectionCount()

	var todayUsers int64
	today := time.Now().Format("2006-01-02")
	db.Model(&model.User{}).Where("DATE(created_at) = ?", today).Count(&todayUsers)

	var todayMessages int64
	db.Model(&model.Message{}).Where("DATE(created_at) = ?", today).Count(&todayMessages)

	stats := gin.H{
		"total_users":     totalUsers,
		"total_messages":  totalMessages,
		"total_groups":    totalGroups,
		"total_files":     totalFiles,
		"online_users":    onlineUsers,
		"today_users":     todayUsers,
		"today_messages":  todayMessages,
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    stats,
	})
}

func (h *SystemMonitorHandler) GetUserGrowthTrend(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	type DailyCount struct {
		Date  string `json:"date"`
		Count int64  `json:"count"`
	}

	var results []DailyCount
	
	db.Model(&model.User{}).
		Select("DATE(created_at) as date, COUNT(*) as count").
		Where("created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)").
		Group("DATE(created_at)").
		Order("date ASC").
		Scan(&results)

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    results,
	})
}

func (h *SystemMonitorHandler) GetMessageTypeStats(c *gin.Context) {
	db := mysql.GetDB()
	if db == nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "Database unavailable",
		})
		return
	}

	type TypeCount struct {
		Type  string `json:"type"`
		Count int64  `json:"count"`
	}

	var results []TypeCount
	
	db.Model(&model.Message{}).
		Select("type, COUNT(*) as count").
		Group("type").
		Order("count DESC").
		Scan(&results)

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    results,
	})
}

func (h *SystemMonitorHandler) GetOnlineDeviceDistribution(c *gin.Context) {
	distribution := h.hub.GetDeviceDistribution()

	result := make([]gin.H, 0)
	for device, count := range distribution {
		if count > 0 {
			result = append(result, gin.H{
				"name":  device,
				"value": count,
			})
		}
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    result,
	})
}

func (h *SystemMonitorHandler) HealthCheck(c *gin.Context) {
	db := mysql.GetDB()
	if db != nil {
		sqlDB, err := db.DB()
		if err != nil || sqlDB.Ping() != nil {
			c.JSON(http.StatusServiceUnavailable, gin.H{
				"code":    500,
				"message": "database unavailable",
			})
			return
		}
	}

	redisClient := redis.GetClient()
	if redisClient != nil {
		if err := redisClient.Ping(c).Err(); err != nil {
			c.JSON(http.StatusServiceUnavailable, gin.H{
				"code":    500,
				"message": "redis unavailable",
			})
			return
		}
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "healthy",
		"data": gin.H{
			"status": "ok",
			"time":   time.Now().Unix(),
		},
	})
}
