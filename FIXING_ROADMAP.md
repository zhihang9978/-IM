# 蓝信IM修复路线图
## 基于审查报告的可执行计划

**制定时间**: 2025-10-18  
**预计总工期**: 2-4周  
**基于文档**: `PROJECT_REVIEW_REPORT.md`

---

# 🎯 修复优先级总览

```
优先级划分:
P0 (严重) → 必须立即修复,影响核心功能
P1 (重要) → 应该尽快修复,影响用户体验
P2 (一般) → 建议修复,优化性能和安全
```

| 优先级 | 问题 | 工期 | 紧急度 |
|--------|------|------|--------|
| **P0** | 群聊功能缺失 | 3-4天 | 🔴🔴🔴🔴🔴 |
| **P0** | WebSocket管理不完善 | 2天 | 🔴🔴🔴🔴☆ |
| **P0** | 消息可靠性不足 | 2-3天 | 🔴🔴🔴🔴☆ |
| **P1** | 缺少Protobuf | 2天 | 🟡🟡🟡☆☆ |
| **P1** | 消息同步机制 | 1天 | 🟡🟡🟡☆☆ |
| **P2** | Cloudflare配置验证 | 0.5天 | 🟢🟢☆☆☆ |

---

# 📋 第一周: P0严重缺陷修复

## 任务1: 实现群聊功能 (3-4天) 🔴

### 任务分解

**Day 1-2: 后端实现**
```
[ ] 1.1 创建群组Service (2-3小时)
[ ] 1.2 创建群组DAO (1-2小时)
[ ] 1.3 创建群组API Handler (2-3小时)
[ ] 1.4 修改消息Service支持群消息 (2-3小时)
[ ] 1.5 测试群组API (1小时)
```

**Day 3: Android客户端**
```
[ ] 1.6 对接群组API (2-3小时)
[ ] 1.7 完善GroupChatActivity (3-4小时)
[ ] 1.8 实现群成员管理 (2小时)
```

**Day 4: 联调测试**
```
[ ] 1.9 端到端测试 (2小时)
[ ] 1.10 修复Bug (2-4小时)
```

### 详细实施步骤

#### Step 1: 创建 `group_service.go`

```go
// apps/backend/internal/service/group_service.go
package service

import (
	"errors"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/websocket"
)

type GroupService struct {
	groupDAO       *dao.GroupDAO
	groupMemberDAO *dao.GroupMemberDAO
	userDAO        *dao.UserDAO
	logDAO         *dao.OperationLogDAO
	hub            *websocket.Hub
}

func NewGroupService(hub *websocket.Hub) *GroupService {
	return &GroupService{
		groupDAO:       dao.NewGroupDAO(),
		groupMemberDAO: dao.NewGroupMemberDAO(),
		userDAO:        dao.NewUserDAO(),
		logDAO:         dao.NewOperationLogDAO(),
		hub:            hub,
	}
}

// CreateGroup 创建群组
func (s *GroupService) CreateGroup(ownerID uint, name, avatar string, memberIDs []uint, ip, userAgent string) (*model.Group, error) {
	// 验证成员是否存在
	for _, memberID := range memberIDs {
		if _, err := s.userDAO.GetByID(memberID); err != nil {
			return nil, errors.New("member not found")
		}
	}
	
	// 创建群组
	group := &model.Group{
		Name:        name,
		Avatar:      avatar,
		OwnerID:     ownerID,
		Type:        model.GroupTypeNormal,
		MemberCount: len(memberIDs) + 1, // +1 包含创建者
	}
	
	if err := s.groupDAO.Create(group); err != nil {
		return nil, err
	}
	
	// 添加群主为成员
	if err := s.addMember(group.ID, ownerID, model.GroupRoleOwner); err != nil {
		return nil, err
	}
	
	// 添加其他成员
	for _, memberID := range memberIDs {
		if err := s.addMember(group.ID, memberID, model.GroupRoleMember); err != nil {
			// 记录错误但继续
			continue
		}
	}
	
	// 记录操作日志
	s.logDAO.CreateLog(dao.LogRequest{
		Action:    model.ActionGroupCreate,
		UserID:    &ownerID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"group_id":     group.ID,
			"group_name":   name,
			"member_count": len(memberIDs) + 1,
		},
		Result: model.ResultSuccess,
	})
	
	// 通知所有成员
	for _, memberID := range append(memberIDs, ownerID) {
		if s.hub.IsUserOnline(memberID) {
			s.hub.SendToUser(memberID, map[string]interface{}{
				"type": "group_created",
				"data": group,
			})
		}
	}
	
	return group, nil
}

// AddMembers 添加群成员
func (s *GroupService) AddMembers(groupID, operatorID uint, memberIDs []uint, ip, userAgent string) error {
	// 验证操作者权限
	role, err := s.groupMemberDAO.GetMemberRole(groupID, operatorID)
	if err != nil || (role != model.GroupRoleOwner && role != model.GroupRoleAdmin) {
		return errors.New("no permission")
	}
	
	// 获取群组信息
	group, err := s.groupDAO.GetByID(groupID)
	if err != nil {
		return err
	}
	
	// 添加成员
	for _, memberID := range memberIDs {
		if err := s.addMember(groupID, memberID, model.GroupRoleMember); err != nil {
			continue
		}
	}
	
	// 更新群成员数量
	group.MemberCount += len(memberIDs)
	s.groupDAO.Update(group)
	
	// 记录日志
	s.logDAO.CreateLog(dao.LogRequest{
		Action:    model.ActionGroupAddMember,
		UserID:    &operatorID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"group_id":   groupID,
			"member_ids": memberIDs,
		},
		Result: model.ResultSuccess,
	})
	
	return nil
}

// SendGroupMessage 发送群消息
func (s *GroupService) SendGroupMessage(groupID, senderID uint, content, msgType string, fileURL *string) (*model.Message, error) {
	// 验证发送者是否是群成员
	if !s.groupMemberDAO.IsMember(groupID, senderID) {
		return nil, errors.New("not a group member")
	}
	
	// 创建消息
	message := &model.Message{
		SenderID:   senderID,
		GroupID:    &groupID,
		Content:    content,
		Type:       msgType,
		Status:     model.MessageStatusSent,
	}
	
	if fileURL != nil {
		message.FileURL = *fileURL
	}
	
	// 保存消息
	if err := s.messageDAO.Create(message); err != nil {
		return nil, err
	}
	
	// 获取所有群成员
	members, err := s.groupMemberDAO.GetMembers(groupID)
	if err != nil {
		return message, nil
	}
	
	// 推送给在线成员
	for _, member := range members {
		if member.UserID != senderID && s.hub.IsUserOnline(member.UserID) {
			s.hub.SendMessageNotification(member.UserID, message)
		}
	}
	
	return message, nil
}

// 内部方法: 添加单个成员
func (s *GroupService) addMember(groupID, userID uint, role string) error {
	member := &model.GroupMember{
		GroupID: groupID,
		UserID:  userID,
		Role:    role,
	}
	return s.groupMemberDAO.Create(member)
}
```

#### Step 2: 创建 `group_dao.go`

```go
// apps/backend/internal/dao/group_dao.go
package dao

import (
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
)

type GroupDAO struct{}

func NewGroupDAO() *GroupDAO {
	return &GroupDAO{}
}

func (d *GroupDAO) Create(group *model.Group) error {
	return mysql.DB.Create(group).Error
}

func (d *GroupDAO) GetByID(id uint) (*model.Group, error) {
	var group model.Group
	err := mysql.DB.Preload("Owner").Preload("Members").First(&group, id).Error
	return &group, err
}

func (d *GroupDAO) Update(group *model.Group) error {
	return mysql.DB.Save(group).Error
}

func (d *GroupDAO) Delete(id uint) error {
	return mysql.DB.Delete(&model.Group{}, id).Error
}

func (d *GroupDAO) GetUserGroups(userID uint) ([]model.Group, error) {
	var groups []model.Group
	err := mysql.DB.
		Joins("JOIN group_members ON groups.id = group_members.group_id").
		Where("group_members.user_id = ?", userID).
		Find(&groups).Error
	return groups, err
}

// GroupMemberDAO
type GroupMemberDAO struct{}

func NewGroupMemberDAO() *GroupMemberDAO {
	return &GroupMemberDAO{}
}

func (d *GroupMemberDAO) Create(member *model.GroupMember) error {
	return mysql.DB.Create(member).Error
}

func (d *GroupMemberDAO) GetMembers(groupID uint) ([]model.GroupMember, error) {
	var members []model.GroupMember
	err := mysql.DB.Preload("User").Where("group_id = ?", groupID).Find(&members).Error
	return members, err
}

func (d *GroupMemberDAO) IsMember(groupID, userID uint) bool {
	var count int64
	mysql.DB.Model(&model.GroupMember{}).
		Where("group_id = ? AND user_id = ?", groupID, userID).
		Count(&count)
	return count > 0
}

func (d *GroupMemberDAO) GetMemberRole(groupID, userID uint) (string, error) {
	var member model.GroupMember
	err := mysql.DB.Where("group_id = ? AND user_id = ?", groupID, userID).First(&member).Error
	return member.Role, err
}

func (d *GroupMemberDAO) RemoveMember(groupID, userID uint) error {
	return mysql.DB.
		Where("group_id = ? AND user_id = ?", groupID, userID).
		Delete(&model.GroupMember{}).Error
}
```

#### Step 3: 创建API Handler

```go
// apps/backend/internal/api/group.go
package api

import (
	"net/http"
	"strconv"
	
	"github.com/gin-gonic/gin"
	"github.com/lanxin/im-backend/internal/middleware"
	"github.com/lanxin/im-backend/internal/service"
	"github.com/lanxin/im-backend/internal/websocket"
)

type GroupHandler struct {
	groupService *service.GroupService
}

func NewGroupHandler(hub *websocket.Hub) *GroupHandler {
	return &GroupHandler{
		groupService: service.NewGroupService(hub),
	}
}

// CreateGroup 创建群组
// POST /api/v1/groups
func (h *GroupHandler) CreateGroup(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	
	var req struct {
		Name      string   `json:"name" binding:"required"`
		Avatar    string   `json:"avatar"`
		MemberIDs []uint   `json:"member_ids" binding:"required"`
	}
	
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}
	
	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")
	
	group, err := h.groupService.CreateGroup(
		userID,
		req.Name,
		req.Avatar,
		req.MemberIDs,
		ip,
		userAgent,
	)
	
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"group": group,
		},
	})
}

// GetGroupInfo 获取群组信息
// GET /api/v1/groups/:id
func (h *GroupHandler) GetGroupInfo(c *gin.Context) {
	groupID, _ := strconv.ParseUint(c.Param("id"), 10, 32)
	
	group, err := h.groupService.GetGroupInfo(uint(groupID))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Group not found",
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"group": group,
		},
	})
}

// GetGroupMembers 获取群成员列表
// GET /api/v1/groups/:id/members
func (h *GroupHandler) GetGroupMembers(c *gin.Context) {
	groupID, _ := strconv.ParseUint(c.Param("id"), 10, 32)
	
	members, err := h.groupService.GetMembers(uint(groupID))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"members": members,
		},
	})
}

// AddMembers 添加群成员
// POST /api/v1/groups/:id/members
func (h *GroupHandler) AddMembers(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	groupID, _ := strconv.ParseUint(c.Param("id"), 10, 32)
	
	var req struct {
		MemberIDs []uint `json:"member_ids" binding:"required"`
	}
	
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}
	
	ip := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")
	
	err := h.groupService.AddMembers(uint(groupID), userID, req.MemberIDs, ip, userAgent)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": err.Error(),
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

// SendGroupMessage 发送群消息
// POST /api/v1/groups/:id/messages
func (h *GroupHandler) SendGroupMessage(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	groupID, _ := strconv.ParseUint(c.Param("id"), 10, 32)
	
	var req struct {
		Content string  `json:"content" binding:"required"`
		Type    string  `json:"type"`
		FileURL *string `json:"file_url"`
	}
	
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "Invalid request",
			"data":    nil,
		})
		return
	}
	
	if req.Type == "" {
		req.Type = "text"
	}
	
	message, err := h.groupService.SendGroupMessage(
		uint(groupID),
		userID,
		req.Content,
		req.Type,
		req.FileURL,
	)
	
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"message": message,
		},
	})
}
```

#### Step 4: 注册路由

```go
// apps/backend/cmd/server/main.go
// 在authorized组中添加群组路由

groupHandler := api.NewGroupHandler(hub)

// 群组相关
authorized.POST("/groups", groupHandler.CreateGroup)
authorized.GET("/groups/:id", groupHandler.GetGroupInfo)
authorized.GET("/groups/:id/members", groupHandler.GetGroupMembers)
authorized.POST("/groups/:id/members", groupHandler.AddMembers)
authorized.DELETE("/groups/:id/members/:user_id", groupHandler.RemoveMember)
authorized.POST("/groups/:id/messages", groupHandler.SendGroupMessage)
authorized.PUT("/groups/:id", groupHandler.UpdateGroup)
authorized.DELETE("/groups/:id", groupHandler.DisbandGroup)
```

#### Step 5: Android客户端对接

```kotlin
// apps/android/.../data/remote/ApiService.kt

// 群组API
@POST("groups")
suspend fun createGroup(@Body request: CreateGroupRequest): ApiResponse<GroupResponse>

@GET("groups/{id}")
suspend fun getGroupInfo(@Path("id") groupId: Long): ApiResponse<GroupResponse>

@GET("groups/{id}/members")
suspend fun getGroupMembers(@Path("id") groupId: Long): ApiResponse<GroupMembersResponse>

@POST("groups/{id}/members")
suspend fun addGroupMembers(
    @Path("id") groupId: Long,
    @Body memberIds: Map<String, List<Long>>
): ApiResponse<Any?>

@POST("groups/{id}/messages")
suspend fun sendGroupMessage(
    @Path("id") groupId: Long,
    @Body request: SendMessageRequest
): ApiResponse<MessageResponse>

// 请求数据类
data class CreateGroupRequest(
    val name: String,
    val avatar: String?,
    val member_ids: List<Long>
)

// 响应数据类
data class GroupResponse(
    val id: Long,
    val name: String,
    val avatar: String?,
    val owner_id: Long,
    val member_count: Int,
    val created_at: Long
)

data class GroupMembersResponse(
    val members: List<GroupMember>
)

data class GroupMember(
    val user_id: Long,
    val role: String,
    val nickname: String?,
    val joined_at: Long
)
```

### 测试清单

```bash
# 后端测试
[ ] 创建群组 POST /api/v1/groups
[ ] 获取群信息 GET /api/v1/groups/:id
[ ] 获取群成员 GET /api/v1/groups/:id/members
[ ] 添加成员 POST /api/v1/groups/:id/members
[ ] 发送群消息 POST /api/v1/groups/:id/messages

# Android测试
[ ] 创建群组界面
[ ] 群聊界面显示
[ ] 发送群消息
[ ] 接收群消息
[ ] 添加群成员
```

---

## 任务2: 完善WebSocket管理 (2天) 🔴

### 实施步骤

#### Step 1: 添加连接池限制

```go
// apps/backend/internal/websocket/hub.go

type Hub struct {
	clients     map[*Client]bool
	userClients map[uint][]*Client
	broadcast   chan []byte
	register    chan *Client
	unregister  chan *Client
	mu sync.RWMutex
	
	// ✅ 新增配置
	maxClients        int           // 最大总连接数
	maxClientsPerUser int           // 单用户最大设备数
	clientTimeout     time.Duration // 连接超时时间
	cleanupTicker     *time.Ticker  // 清理定时器
	lastActivity      map[*Client]time.Time // 最后活动时间
}

func NewHub(maxClients, maxClientsPerUser int, timeout time.Duration) *Hub {
	h := &Hub{
		clients:           make(map[*Client]bool),
		userClients:       make(map[uint][]*Client),
		broadcast:         make(chan []byte, 10000), // 扩大buffer
		register:          make(chan *Client),
		unregister:        make(chan *Client),
		maxClients:        maxClients,
		maxClientsPerUser: maxClientsPerUser,
		clientTimeout:     timeout,
		lastActivity:      make(map[*Client]time.Time),
	}
	
	// 启动清理协程
	h.startCleanupRoutine()
	
	return h
}

// 检查是否可以接受新连接
func (h *Hub) canAcceptConnection(userID uint) bool {
	h.mu.RLock()
	defer h.mu.RUnlock()
	
	// 检查总连接数
	if len(h.clients) >= h.maxClients {
		log.Printf("Max clients reached: %d", len(h.clients))
		return false
	}
	
	// 检查单用户设备数
	if len(h.userClients[userID]) >= h.maxClientsPerUser {
		log.Printf("Max clients per user reached for user %d", userID)
		return false
	}
	
	return true
}

// 定期清理僵尸连接
func (h *Hub) startCleanupRoutine() {
	h.cleanupTicker = time.NewTicker(30 * time.Second)
	go func() {
		for range h.cleanupTicker.C {
			h.cleanupStaleConnections()
		}
	}()
}

func (h *Hub) cleanupStaleConnections() {
	h.mu.Lock()
	defer h.mu.Unlock()
	
	now := time.Now()
	for client, _ := range h.clients {
		if lastActive, ok := h.lastActivity[client]; ok {
			if now.Sub(lastActive) > h.clientTimeout {
				// 超时,关闭连接
				log.Printf("Closing stale connection for user %d", client.userID)
				close(client.send)
				delete(h.clients, client)
				delete(h.lastActivity, client)
				h.removeClientFromUser(client)
			}
		}
	}
}

// 更新活动时间
func (h *Hub) updateActivity(client *Client) {
	h.mu.Lock()
	defer h.mu.Unlock()
	h.lastActivity[client] = time.Now()
}
```

#### Step 2: 修改连接处理

```go
// apps/backend/internal/websocket/client.go

func ServeWS(hub *Hub, c *gin.Context, secret string) {
	// 验证token
	tokenString := c.Query("token")
	if tokenString == "" {
		c.JSON(401, gin.H{"code": 401, "message": "Token required"})
		return
	}
	
	claims, err := jwt.ParseToken(tokenString, secret)
	if err != nil {
		c.JSON(401, gin.H{"code": 401, "message": "Invalid token"})
		return
	}
	
	// ✅ 检查是否可以接受新连接
	if !hub.canAcceptConnection(claims.UserID) {
		c.JSON(503, gin.H{
			"code":    503,
			"message": "Server too busy or too many devices",
			"data":    nil,
		})
		return
	}
	
	// 升级连接
	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		log.Printf("Failed to upgrade connection: %v", err)
		return
	}
	
	client := &Client{
		hub:      hub,
		conn:     conn,
		send:     make(chan []byte, 256),
		userID:   claims.UserID,
		username: claims.Username,
	}
	
	client.hub.register <- client
	
	go client.writePump()
	go client.readPump()
}

// readPump中更新活动时间
func (c *Client) readPump() {
	defer func() {
		c.hub.unregister <- c
		c.conn.Close()
	}()
	
	c.conn.SetReadLimit(maxMessageSize)
	c.conn.SetReadDeadline(time.Now().Add(pongWait))
	c.conn.SetPongHandler(func(string) error {
		c.conn.SetReadDeadline(time.Now().Add(pongWait))
		c.hub.updateActivity(c) // ✅ 更新活动时间
		return nil
	})
	
	for {
		_, message, err := c.conn.ReadMessage()
		if err != nil {
			if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
				log.Printf("WebSocket error: %v", err)
			}
			break
		}
		
		c.hub.updateActivity(c) // ✅ 更新活动时间
		c.handleMessage(message)
	}
}
```

#### Step 3: 配置文件

```yaml
# config.yaml
websocket:
  max_connections: 10000      # 最大总连接数
  max_connections_per_user: 5 # 单用户最大设备数
  connection_timeout: 300     # 连接超时(秒)
  read_buffer_size: 1024
  write_buffer_size: 1024
  broadcast_buffer: 10000     # broadcast channel buffer
  heartbeat_interval: 30
  max_message_size: 10240
```

### 测试

```bash
# 压力测试
[ ] 模拟10000并发连接
[ ] 模拟单用户5个设备
[ ] 测试僵尸连接清理
[ ] 测试连接限制
```

---

## 任务3: 消息可靠性保证 (2-3天) 🔴

### 实施步骤

#### Step 1: 添加消息ACK机制

```go
// 1. 添加ACK API
// apps/backend/internal/api/message.go

// AckDelivered 确认消息已送达
// POST /api/v1/messages/:id/ack/delivered
func (h *MessageHandler) AckDelivered(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	messageID, _ := strconv.ParseUint(c.Param("id"), 10, 32)
	
	message, err := h.messageService.GetMessage(uint(messageID))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Message not found",
			"data":    nil,
		})
		return
	}
	
	// 只有接收者能确认
	if message.ReceiverID != userID {
		c.JSON(http.StatusForbidden, gin.H{
			"code":    403,
			"message": "Not authorized",
			"data":    nil,
		})
		return
	}
	
	// 更新消息状态为已送达
	err = h.messageService.UpdateStatus(uint(messageID), model.MessageStatusDelivered)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}
	
	// 通知发送者
	h.hub.SendMessageStatusUpdate(message.SenderID, uint(messageID), "delivered")
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    nil,
	})
}

// AckRead 确认消息已读
// POST /api/v1/messages/:id/ack/read
func (h *MessageHandler) AckRead(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	messageID, _ := strconv.ParseUint(c.Param("id"), 10, 32)
	
	message, err := h.messageService.GetMessage(uint(messageID))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"code":    404,
			"message": "Message not found",
			"data":    nil,
		})
		return
	}
	
	if message.ReceiverID != userID {
		c.JSON(http.StatusForbidden, gin.H{
			"code":    403,
			"message": "Not authorized",
			"data":    nil,
		})
		return
	}
	
	// 更新消息状态为已读
	err = h.messageService.UpdateStatus(uint(messageID), model.MessageStatusRead)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}
	
	// 通知发送者
	h.hub.SendMessageStatusUpdate(message.SenderID, uint(messageID), "read")
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    nil,
	})
}
```

#### Step 2: 离线消息队列

```go
// apps/backend/internal/service/message_service.go

func (s *MessageService) SendMessage(...) (*model.Message, error) {
	// ... 创建消息 ...
	
	// 检查接收者是否在线
	if s.hub.IsUserOnline(receiverID) {
		// 在线:尝试推送
		err := s.hub.SendMessageNotification(receiverID, message)
		if err != nil {
			// 推送失败,存入离线队列
			s.saveOfflineMessage(receiverID, message)
		}
	} else {
		// 离线:存入离线队列
		s.saveOfflineMessage(receiverID, message)
	}
	
	return message, nil
}

// 保存离线消息
func (s *MessageService) saveOfflineMessage(userID uint, message *model.Message) {
	key := fmt.Sprintf("offline_msg:%d", userID)
	
	// 存入Redis List
	s.redis.RPush(context.Background(), key, message.ID)
	
	// 设置7天过期
	s.redis.Expire(context.Background(), key, 7*24*time.Hour)
}

// 获取离线消息
func (s *MessageService) GetOfflineMessages(userID uint) ([]model.Message, error) {
	key := fmt.Sprintf("offline_msg:%d", userID)
	
	// 从Redis读取消息ID列表
	messageIDs, err := s.redis.LRange(context.Background(), key, 0, -1).Result()
	if err != nil {
		return nil, err
	}
	
	messages := []model.Message{}
	for _, idStr := range messageIDs {
		id, _ := strconv.ParseUint(idStr, 10, 32)
		msg, err := s.messageDAO.GetByID(uint(id))
		if err == nil {
			messages = append(messages, *msg)
		}
	}
	
	// 清空队列
	s.redis.Del(context.Background(), key)
	
	return messages, nil
}

// 用户上线时拉取离线消息API
// GET /api/v1/messages/offline
func (h *MessageHandler) GetOfflineMessages(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	
	messages, err := h.messageService.GetOfflineMessages(userID)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": err.Error(),
			"data":    nil,
		})
		return
	}
	
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"messages": messages,
			"count":    len(messages),
		},
	})
}
```

#### Step 3: Android客户端ACK

```kotlin
// WebSocketClient收到消息后立即ACK

override fun onMessage(text: String) {
    val message = gson.fromJson(text, WebSocketMessage::class.java)
    
    when (message.type) {
        "message" -> {
            val msg = message.data as Message
            
            // 保存到本地数据库
            messageDao.insert(msg)
            
            // ✅ 立即发送送达ACK
            apiService.ackDelivered(msg.id)
            
            // 显示通知
            showNotification(msg)
            
            // 如果在聊天界面,标记已读
            if (isInChat(msg.conversation_id)) {
                apiService.ackRead(msg.id)
            }
        }
    }
}

// 用户打开聊天界面时
fun onChatOpened(conversationId: Long) {
    // 获取该会话所有未读消息
    val unreadMessages = messageDao.getUnreadMessages(conversationId)
    
    // 批量标记已读
    unreadMessages.forEach { msg ->
        apiService.ackRead(msg.id)
    }
}

// App启动时拉取离线消息
fun onAppLaunched() {
    lifecycleScope.launch {
        val offlineMessages = apiService.getOfflineMessages()
        offlineMessages.forEach { msg ->
            messageDao.insert(msg)
            apiService.ackDelivered(msg.id)
        }
    }
}
```

### 测试清单

```bash
# 测试送达ACK
[ ] 发送消息→接收者在线→立即ACK→发送者收到"已送达"

# 测试已读ACK
[ ] 接收者打开聊天界面→发送已读ACK→发送者收到"已读"

# 测试离线消息
[ ] 接收者离线→发送消息→存入队列→接收者上线→拉取消息→ACK

# 测试推送失败重试
[ ] 推送失败→自动存入离线队列→接收者拉取
```

---

# 📋 第二周: P1重要优化

## 任务4: Protocol Buffers实现 (2天) 🟡

(详细实施步骤见完整文档...)

## 任务5: 消息同步机制 (1天) 🟡

(详细实施步骤见完整文档...)

## 任务6: Cloudflare TLS配置验证 (0.5天) 🟢

### 实施步骤

#### Step 1: Cloudflare Dashboard检查

```
登录: https://dash.cloudflare.com
域名: lanxin168.com

检查项:
[ ] SSL/TLS → Overview → Full (strict) ✅
[ ] SSL/TLS → Edge Certificates → Always Use HTTPS ✅
[ ] SSL/TLS → Edge Certificates → Minimum TLS Version: 1.2+
[ ] SSL/TLS → Edge Certificates → TLS 1.3: Enabled
[ ] SSL/TLS → Edge Certificates → HSTS: Enabled
[ ] Security → WAF: Enabled
[ ] Security → DDoS: Automatic (默认启用)
```

#### Step 2: 后端配置更新

```yaml
# apps/backend/config/config.yaml
server:
  port: 8080
  mode: release  # 生产环境
  domain: lanxin168.com
  
  # ✅ 强制HTTPS
  force_https: true
  
  # ✅ HSTS设置
  hsts_max_age: 31536000  # 1年
  hsts_include_subdomains: true
  hsts_preload: true

security:
  # ✅ TLS配置
  tls:
    min_version: "1.2"
    prefer_server_cipher_suites: true
    
  # ✅ 信任Cloudflare代理
  trusted_proxies:
    - "173.245.48.0/20"
    - "103.21.244.0/22"
    - "103.22.200.0/22"
    - "103.31.4.0/22"
    - "141.101.64.0/18"
    - "108.162.192.0/18"
    - "190.93.240.0/20"
    - "188.114.96.0/20"
    - "197.234.240.0/22"
    - "198.41.128.0/17"
    - "162.158.0.0/15"
    - "104.16.0.0/13"
    - "104.24.0.0/14"
    - "172.64.0.0/13"
    - "131.0.72.0/22"
```

#### Step 3: Gin中间件配置

```go
// apps/backend/cmd/server/main.go

import (
    "github.com/gin-gonic/gin"
    "github.com/unrolled/secure"
)

func main() {
    r := gin.Default()
    
    // ✅ 安全中间件
    secureMiddleware := secure.New(secure.Options{
        SSLRedirect:          true,  // HTTP→HTTPS重定向
        SSLHost:              "api.lanxin168.com",
        STSSeconds:           31536000,
        STSIncludeSubdomains: true,
        STSPreload:           true,
        FrameDeny:            true,
        ContentTypeNosniff:   true,
        BrowserXssFilter:     true,
        ContentSecurityPolicy: "default-src 'self'",
        IsDevelopment:        cfg.Server.Mode == "debug",
    })
    
    r.Use(func(c *gin.Context) {
        err := secureMiddleware.Process(c.Writer, c.Request)
        if err != nil {
            c.Abort()
            return
        }
        c.Next()
    })
    
    // ✅ 信任Cloudflare代理
    r.SetTrustedProxies(cfg.Security.TrustedProxies)
    
    // ... 其他路由 ...
}
```

#### Step 4: WebSocket WSS配置

```kotlin
// apps/android/app/src/main/java/com/lanxin/im/data/remote/WebSocketClient.kt

class WebSocketClient {
    companion object {
        // ✅ 必须使用wss://（安全WebSocket）
        private const val WS_URL = "wss://api.lanxin168.com/ws"
        // ❌ 不要使用ws://（非加密）
    }
    
    private fun createWebSocket(token: String): WebSocket {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // ✅ OkHttp自动处理WSS加密
            .build()
        
        val request = Request.Builder()
            .url("$WS_URL?token=$token")
            .build()
        
        return client.newWebSocket(request, this)
    }
}
```

#### Step 5: SSL测试

```bash
# 1. 测试HTTPS连接
curl -v https://api.lanxin168.com/health

期望输出:
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384
* Server certificate:
*  subject: CN=*.lanxin168.com
*  issuer: C=US; O=Cloudflare, Inc.; CN=Cloudflare Inc ECC CA-3
*  SSL certificate verify ok.

# 2. 测试HTTP→HTTPS重定向
curl -I http://api.lanxin168.com/health

期望输出:
HTTP/1.1 301 Moved Permanently
Location: https://api.lanxin168.com/health

# 3. 测试HSTS头
curl -I https://api.lanxin168.com/health | grep -i strict

期望输出:
strict-transport-security: max-age=31536000; includeSubDomains; preload

# 4. SSL Labs完整测试
访问: https://www.ssllabs.com/ssltest/analyze.html?d=api.lanxin168.com

期望评分:
Overall Rating: A+ ✅
Certificate: 100
Protocol Support: 100
Key Exchange: 100
Cipher Strength: 90

# 5. 测试WebSocket加密
使用Chrome DevTools:
const ws = new WebSocket('wss://api.lanxin168.com/ws?token=xxx');
ws.onopen = () => console.log('✅ WSS连接成功');

期望: 
- 连接成功
- Network面板显示wss://协议（绿色小锁图标）
```

#### Step 6: 安全header验证

```bash
# 检查所有安全header
curl -I https://api.lanxin168.com/health

期望包含:
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
CF-Ray: xxx-xxx  # Cloudflare标识
```

### 验收标准

```
[ ] Cloudflare SSL设置为Full (strict)
[ ] HTTPS强制重定向工作正常
[ ] HSTS头正确配置
[ ] TLS 1.3启用
[ ] SSL Labs评分A+
[ ] WebSocket使用wss://协议
[ ] 所有安全header正确
[ ] 信任Cloudflare代理IP
```

### 安全等级评估

**传输层安全**: ⭐⭐⭐⭐⭐ 5/5
- Cloudflare全球CDN
- TLS 1.3最新标准
- 自动DDoS防护
- WAF防护

**认证安全**: ⭐⭐⭐⭐⭐ 5/5  
- JWT Token
- bcrypt密码哈希
- Token黑名单

**存储安全**: ⭐⭐⭐☆☆ 3/5
- 明文存储（企业IM标准）
- 适合需要审计的企业场景

**综合安全等级**: ⭐⭐⭐⭐☆ 4.3/5
**结论**: 符合企业级IM安全标准，与微信企业版/钉钉同级别 ✅

---

# 📊 进度跟踪表

| 任务 | 预计工期 | 实际工期 | 状态 | 备注 |
|------|---------|---------|------|------|
| 群聊功能 | 3-4天 | ___ | ⏳ 未开始 | |
| WebSocket管理 | 2天 | ___ | ⏳ 未开始 | |
| 消息可靠性 | 2-3天 | ___ | ⏳ 未开始 | |
| Protobuf | 2天 | ___ | ⏳ 未开始 | |
| 消息同步 | 1天 | ___ | ⏳ 未开始 | |
| E2EE | 5-7天 | ___ | ⏳ 未开始 | 可选 |

---

# ✅ 验收标准

## P0任务验收

### 群聊功能
- [ ] 可以创建群组
- [ ] 可以发送群消息
- [ ] 可以添加/移除成员
- [ ] 在线成员实时收到消息
- [ ] 离线成员上线后能看到消息

### WebSocket管理
- [ ] 支持10000+并发连接
- [ ] 僵尸连接自动清理
- [ ] 连接数限制生效
- [ ] 压力测试通过

### 消息可靠性
- [ ] 消息送达ACK工作正常
- [ ] 消息已读ACK工作正常
- [ ] 离线消息队列工作正常
- [ ] 消息不丢失

---

# 📞 支持资源

## 参考文档
- `PROJECT_REVIEW_REPORT.md` - 详细审查报告
- `IM全栈开发完整知识库.md` - 完整IM知识
- `apps/backend/API_DOCUMENTATION.md` - API文档

## 技术支持
- 野火IM源码: `android-chat-master/`
- 野火IM官方文档: https://docs.wildfirechat.cn/

## 测试工具
- WebSocket测试: wscat, Browser Console
- API测试: Postman, curl
- 压力测试: JMeter, Locust

---

**创建时间**: 2025-10-18  
**下次更新**: 完成P0任务后

