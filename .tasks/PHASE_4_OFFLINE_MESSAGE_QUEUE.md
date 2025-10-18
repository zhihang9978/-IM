# 阶段4: 实现离线消息队列
## 单一目标 - Redis队列 - 完整逻辑

**阶段编号**: Phase 4 of 5  
**预计时间**: 1-2小时  
**前置条件**: 阶段1-3完成  
**成功标准**: 离线用户能收到消息,上线后自动拉取

---

# 🎯 本阶段唯一目标

**实现**: 完整的离线消息处理机制

**当前问题**:
```go
if s.hub.IsUserOnline(receiverID) {
    // 在线: 推送 ✅
} else {
    // 离线: 什么都不做 ❌
}
```

**修复后**:
```go
if s.hub.IsUserOnline(receiverID) {
    // 在线: 推送 ✅
} else {
    // 离线: 存入Redis队列 ✅
}
// 用户上线时: 自动拉取离线消息 ✅
```

---

# 📋 修复步骤

## Step 1: 修改MessageService添加离线消息方法 (30分钟)

### 文件: `apps/backend/internal/service/message_service.go`

### 操作1: 添加import

确保有:
```go
import (
	"context"
	"errors"
	"fmt"        // ✅ 新增(如果没有)
	"strconv"    // ✅ 新增(如果没有)
	"time"
	
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/redis"  // ✅ 新增
	"github.com/lanxin/im-backend/internal/websocket"
	"github.com/lanxin/im-backend/pkg/kafka"
)
```

### 操作2: 修改MessageService结构体

找到:
```go
type MessageService struct {
	messageDAO      *dao.MessageDAO
	conversationDAO *dao.ConversationDAO
	userDAO         *dao.UserDAO
	logDAO          *dao.OperationLogDAO
	hub             *websocket.Hub
	producer        *kafka.Producer
}
```

添加:
```go
type MessageService struct {
	messageDAO      *dao.MessageDAO
	conversationDAO *dao.ConversationDAO
	userDAO         *dao.UserDAO
	logDAO          *dao.OperationLogDAO
	hub             *websocket.Hub
	producer        *kafka.Producer
	redisClient     *redis.Client  // ✅ 新增
}
```

### 操作3: 修改构造函数

找到:
```go
func NewMessageService(hub *websocket.Hub, producer *kafka.Producer) *MessageService {
	return &MessageService{
		messageDAO:      dao.NewMessageDAO(),
		conversationDAO: dao.NewConversationDAO(),
		userDAO:         dao.NewUserDAO(),
		logDAO:          dao.NewOperationLogDAO(),
		hub:             hub,
		producer:        producer,
	}
}
```

替换为:
```go
func NewMessageService(hub *websocket.Hub, producer *kafka.Producer) *MessageService {
	return &MessageService{
		messageDAO:      dao.NewMessageDAO(),
		conversationDAO: dao.NewConversationDAO(),
		userDAO:         dao.NewUserDAO(),
		logDAO:          dao.NewOperationLogDAO(),
		hub:             hub,
		producer:        producer,
		redisClient:     redis.GetClient(), // ✅ 新增
	}
}
```

### 操作4: 在文件末尾添加离线消息方法

```go
// saveToOfflineQueue 保存消息到离线队列
func (s *MessageService) saveToOfflineQueue(userID uint, messageID uint) error {
	key := fmt.Sprintf("offline_msg:%d", userID)
	ctx := context.Background()
	
	// 存入Redis List (RPUSH = 从右边插入)
	err := s.redisClient.RPush(ctx, key, messageID).Err()
	if err != nil {
		return err
	}
	
	// 设置7天过期
	s.redisClient.Expire(ctx, key, 7*24*time.Hour)
	
	return nil
}

// GetOfflineMessages 获取用户的离线消息
func (s *MessageService) GetOfflineMessages(userID uint) ([]model.Message, error) {
	key := fmt.Sprintf("offline_msg:%d", userID)
	ctx := context.Background()
	
	// 从Redis读取所有消息ID
	messageIDs, err := s.redisClient.LRange(ctx, key, 0, -1).Result()
	if err != nil {
		return nil, err
	}
	
	if len(messageIDs) == 0 {
		return []model.Message{}, nil
	}
	
	// 从数据库加载完整消息
	messages := []model.Message{}
	for _, idStr := range messageIDs {
		id, err := strconv.ParseUint(idStr, 10, 32)
		if err != nil {
			continue
		}
		
		msg, err := s.messageDAO.GetByID(uint(id))
		if err == nil {
			messages = append(messages, *msg)
		}
	}
	
	// 清空离线队列
	s.redisClient.Del(ctx, key)
	
	return messages, nil
}
```

---

## Step 2: 修改SendMessage添加离线消息逻辑 (15分钟)

### 文件: `apps/backend/internal/service/message_service.go`

### 操作: 找到WebSocket推送部分

找到这段代码:
```go
	// 通过WebSocket实时推送给接收者
	go func() {
		if s.hub.IsUserOnline(receiverID) {
			s.hub.SendMessageNotification(receiverID, message)
			// 更新消息状态为已送达
			s.messageDAO.UpdateStatus(message.ID, model.MessageStatusDelivered)
		}
	}()
```

替换为:
```go
	// 通过WebSocket实时推送给接收者
	go func() {
		if s.hub.IsUserOnline(receiverID) {
			// 在线: 尝试推送
			err := s.hub.SendMessageNotification(receiverID, message)
			if err == nil {
				// 推送成功,更新状态为已送达
				s.messageDAO.UpdateStatus(message.ID, model.MessageStatusDelivered)
			} else {
				// 推送失败,存入离线队列
				s.saveToOfflineQueue(receiverID, message.ID)
			}
		} else {
			// ✅ 离线: 存入离线消息队列
			s.saveToOfflineQueue(receiverID, message.ID)
		}
	}()
```

### 验证Step 2

```bash
go build cmd/server/main.go
# 期望: 编译成功
```

---

## Step 3: 添加GetOfflineMessages API (15分钟)

### 文件: `apps/backend/internal/api/message.go`

### 操作: 在文件末尾添加方法

```go
// GetOfflineMessages 获取离线消息
// GET /api/v1/messages/offline
func (h *MessageHandler) GetOfflineMessages(c *gin.Context) {
	userID, _ := middleware.GetUserID(c)
	
	messages, err := h.messageService.GetOfflineMessages(userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
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

---

## Step 4: 注册离线消息路由 (5分钟)

### 文件: `apps/backend/cmd/server/main.go`

### 操作: 在authorized组中添加

找到:
```go
		// 消息相关
		authorized.POST("/messages", messageHandler.SendMessage)
		authorized.POST("/messages/:id/recall", messageHandler.RecallMessage)
		authorized.GET("/conversations/:id/messages", messageHandler.GetMessages)
		authorized.GET("/conversations/:id/messages/history", messageHandler.GetHistoryMessages)
		authorized.GET("/messages/search", messageHandler.SearchMessages)
		authorized.POST("/conversations/:id/read", messageHandler.MarkAsRead)
```

在这段代码后面添加:
```go
		authorized.GET("/messages/offline", messageHandler.GetOfflineMessages) // ✅ 新增
```

### 验证Step 4

```bash
go build cmd/server/main.go
# 期望: 编译成功
```

---

## Step 5: 功能测试 (30分钟)

### 启动服务器

```bash
cd D:\im-lanxin\apps\backend
go run cmd/server/main.go
```

### 测试用例1: 离线消息存储

```bash
# 前置条件: 确保用户2未连接WebSocket(离线状态)

# 步骤1: 用户1发消息给用户2(离线)
curl -X POST http://localhost:8080/api/v1/messages \
  -H "Authorization: Bearer USER1_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"receiver_id\":2,\"content\":\"离线消息测试\",\"type\":\"text\"}"

# ✅ 期望: 消息发送成功

# 步骤2: 检查Redis离线队列
redis-cli
> LRANGE offline_msg:2 0 -1
# ✅ 期望: 返回消息ID列表,如 ["1","2","3"]

> TTL offline_msg:2
# ✅ 期望: 返回剩余过期时间,约604800秒(7天)
```

### 测试用例2: 拉取离线消息

```bash
# 步骤3: 用户2登录并拉取离线消息
curl http://localhost:8080/api/v1/messages/offline \
  -H "Authorization: Bearer USER2_TOKEN"

# ✅ 期望返回:
# {
#   "code": 0,
#   "message": "success",
#   "data": {
#     "messages": [
#       {
#         "id": 1,
#         "content": "离线消息测试",
#         ...
#       }
#     ],
#     "count": 1
#   }
# }

# 步骤4: 检查Redis队列是否清空
redis-cli
> LRANGE offline_msg:2 0 -1
# ✅ 期望: 返回空数组 (nil)
# 因为拉取后自动清空
```

### 测试用例3: 在线推送失败转离线

```bash
# 模拟场景: 用户在线但WebSocket推送失败

# 这个需要手动测试:
# 1. 用户2连接WebSocket
# 2. 手动断开WebSocket连接(但不登出)
# 3. 在hub.IsUserOnline()返回true但SendMessage失败的情况下
# 4. 消息应该存入离线队列

# 验证: 检查Redis是否有离线消息
```

---

## Step 6: 数据验证 (5分钟)

```sql
-- MySQL验证
USE lanxin_im;

-- 检查离线期间发送的消息
SELECT id, conversation_id, sender_id, receiver_id, content, status
FROM messages
WHERE receiver_id = 2
ORDER BY id DESC
LIMIT 10;

-- 期望: 
-- 所有消息都有正确的conversation_id
-- status可能是'sent'(还未送达)
```

---

## Step 7: 提交代码 (5分钟)

```bash
git add apps/backend/internal/service/message_service.go
git add apps/backend/internal/api/message.go
git add apps/backend/cmd/server/main.go

git commit -m "feat: 实现离线消息队列机制

新增功能:
- saveToOfflineQueue: 保存消息到Redis离线队列
- GetOfflineMessages: 拉取用户的所有离线消息
- API: GET /messages/offline

修改:
- SendMessage: 离线用户消息存入队列
- SendMessage: 在线推送失败也存入队列

实现细节:
- 使用Redis List存储离线消息ID
- 队列7天自动过期
- 拉取后自动清空队列

测试通过:
- 离线消息正确存储到Redis
- 拉取API返回所有离线消息
- 拉取后队列自动清空"
```

---

# ✅ 阶段4验收

## 必须全部通过 (10/10)

```
[ ] 1. 代码编译成功
[ ] 2. 服务器正常启动
[ ] 3. 离线发送消息成功
[ ] 4. Redis队列有消息ID
[ ] 5. Redis队列有过期时间(7天)
[ ] 6. 拉取离线消息API成功
[ ] 7. 返回完整的消息列表
[ ] 8. Redis队列拉取后清空
[ ] 9. 数据库消息记录正确
[ ] 10. Git提交完成
```

---

# 📊 阶段4完成标志

```
✅ 离线用户消息存入Redis队列
✅ 用户可以拉取离线消息
✅ 拉取后队列自动清空
✅ 消息不会丢失
```

**如果本阶段全部通过,请继续**: `PHASE_5_ANDROID_INTEGRATION.md`  
**如果本阶段有任何失败,请停止并修复**

---

**文档版本**: 1.0  
**创建时间**: 2025-10-18  
**预计完成时间**: 1-2小时  
**实际完成时间**: ________  
**验收结果**: ⬜ 通过 / ⬜ 失败

