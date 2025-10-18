# 阶段2: 实现群聊功能
## 单一目标 - 完整实现 - 严格测试

**阶段编号**: Phase 2 of 5  
**预计时间**: 2-3小时  
**前置条件**: 阶段1完成并验收通过  
**成功标准**: 能创建群组、发送群消息、管理成员

---

# 🎯 本阶段唯一目标

**实现**: 完整的群聊功能(DAO + Service + API + 路由)

**当前状态**:
```
❌ 无group_dao.go
❌ 无group_service.go
❌ 无group.go API
❌ 主路由无群组路由
```

**修复后**:
```
✅ 完整的群聊DAO
✅ 完整的群聊Service
✅ 完整的群聊API
✅ 8个群组路由
```

---

# 📋 修复步骤 (严格按顺序执行)

## Step 1: 创建group_dao.go (30分钟)

### 操作: 创建新文件

```bash
# 文件路径
apps/backend/internal/dao/group_dao.go
```

### 完整代码 (直接复制)

```go
package dao

import (
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"gorm.io/gorm"
)

// ========== GroupDAO ==========

type GroupDAO struct {
	db *gorm.DB
}

func NewGroupDAO() *GroupDAO {
	return &GroupDAO{
		db: mysql.GetDB(),
	}
}

// Create 创建群组
func (d *GroupDAO) Create(group *model.Group) error {
	return d.db.Create(group).Error
}

// GetByID 根据ID获取群组（含完整信息）
func (d *GroupDAO) GetByID(id uint) (*model.Group, error) {
	var group model.Group
	err := d.db.
		Preload("Owner").
		Preload("Members").
		Preload("Members.User").
		Where("id = ?", id).
		First(&group).Error
	return &group, err
}

// Update 更新群组信息
func (d *GroupDAO) Update(group *model.Group) error {
	return d.db.Save(group).Error
}

// Delete 删除群组（软删除）
func (d *GroupDAO) Delete(id uint) error {
	return d.db.Delete(&model.Group{}, id).Error
}

// GetUserGroups 获取用户加入的所有群组
func (d *GroupDAO) GetUserGroups(userID uint) ([]model.Group, error) {
	var groups []model.Group
	err := d.db.
		Joins("JOIN group_members ON groups.id = group_members.group_id").
		Where("group_members.user_id = ? AND groups.status = ?", userID, model.GroupStatusActive).
		Preload("Owner").
		Find(&groups).Error
	return groups, err
}

// UpdateMemberCount 更新群成员数量
func (d *GroupDAO) UpdateMemberCount(groupID uint, count int) error {
	return d.db.Model(&model.Group{}).
		Where("id = ?", groupID).
		Update("member_count", count).Error
}

// ========== GroupMemberDAO ==========

type GroupMemberDAO struct {
	db *gorm.DB
}

func NewGroupMemberDAO() *GroupMemberDAO {
	return &GroupMemberDAO{
		db: mysql.GetDB(),
	}
}

// Create 添加群成员
func (d *GroupMemberDAO) Create(member *model.GroupMember) error {
	return d.db.Create(member).Error
}

// GetMembers 获取群组所有成员
func (d *GroupMemberDAO) GetMembers(groupID uint) ([]model.GroupMember, error) {
	var members []model.GroupMember
	err := d.db.
		Preload("User").
		Where("group_id = ?", groupID).
		Find(&members).Error
	return members, err
}

// IsMember 检查用户是否是群成员
func (d *GroupMemberDAO) IsMember(groupID, userID uint) bool {
	var count int64
	d.db.Model(&model.GroupMember{}).
		Where("group_id = ? AND user_id = ?", groupID, userID).
		Count(&count)
	return count > 0
}

// GetMemberRole 获取成员角色
func (d *GroupMemberDAO) GetMemberRole(groupID, userID uint) (string, error) {
	var member model.GroupMember
	err := d.db.
		Where("group_id = ? AND user_id = ?", groupID, userID).
		First(&member).Error
	return member.Role, err
}

// RemoveMember 移除群成员
func (d *GroupMemberDAO) RemoveMember(groupID, userID uint) error {
	return d.db.
		Where("group_id = ? AND user_id = ?", groupID, userID).
		Delete(&model.GroupMember{}).Error
}

// GetMemberCount 获取群成员数量
func (d *GroupMemberDAO) GetMemberCount(groupID uint) (int64, error) {
	var count int64
	err := d.db.Model(&model.GroupMember{}).
		Where("group_id = ?", groupID).
		Count(&count).Error
	return count, err
}
```

### 验证Step 1

```bash
go build cmd/server/main.go
# 期望: 编译成功
```

---

## Step 2: 创建group_service.go (1小时)

### 操作: 创建新文件

```bash
# 文件路径
apps/backend/internal/service/group_service.go
```

### 完整代码 (直接复制)

由于代码较长,详见文件: `CODE_ISSUES_AND_FIXES.md` 中的完整group_service.go代码

关键方法:
- `CreateGroup()` - 创建群组
- `GetGroupInfo()` - 获取群组信息
- `GetMembers()` - 获取成员列表
- `AddMembers()` - 添加成员
- `RemoveMember()` - 移除成员
- `SendGroupMessage()` - 发送群消息
- `UpdateGroup()` - 更新群信息
- `DisbandGroup()` - 解散群组

### 验证Step 2

```bash
go build cmd/server/main.go
# 期望: 编译成功
```

---

## Step 3: 创建group.go API Handler (30分钟)

### 操作: 创建新文件

```bash
# 文件路径
apps/backend/internal/api/group.go
```

### 完整代码

详见 `CODE_ISSUES_AND_FIXES.md` 中的完整group.go代码

包含8个API:
- `CreateGroup()` - POST /groups
- `GetGroupInfo()` - GET /groups/:id
- `GetGroupMembers()` - GET /groups/:id/members
- `AddMembers()` - POST /groups/:id/members
- `RemoveMember()` - DELETE /groups/:id/members/:user_id
- `SendGroupMessage()` - POST /groups/:id/messages
- `UpdateGroup()` - PUT /groups/:id
- `DisbandGroup()` - DELETE /groups/:id

### 验证Step 3

```bash
go build cmd/server/main.go
# 期望: 编译成功
```

---

## Step 4: 注册群组路由 (10分钟)

### 文件: `apps/backend/cmd/server/main.go`

### 操作: 在setupRouter函数中添加

找到:
```go
	// 创建Handler
	authHandler := api.NewAuthHandler(cfg)
	userHandler := api.NewUserHandler()
	messageHandler := api.NewMessageHandler(hub, producer)
	fileHandler, _ := api.NewFileHandler(cfg)
	trtcHandler := api.NewTRTCHandler(cfg, hub)
	conversationHandler := api.NewConversationHandler()
	contactHandler := api.NewContactHandler()
	favoriteHandler := api.NewFavoriteHandler()
	reportHandler := api.NewReportHandler()
```

在这段代码后面添加:
```go
	groupHandler := api.NewGroupHandler(hub) // ✅ 新增
```

然后找到:
```go
		// 举报相关
		authorized.POST("/messages/report", reportHandler.ReportMessage)
		authorized.GET("/reports", reportHandler.GetReports)
```

在这段代码后面添加:
```go
		// ✅ 群组相关（新增）
		authorized.POST("/groups", groupHandler.CreateGroup)
		authorized.GET("/groups/:id", groupHandler.GetGroupInfo)
		authorized.GET("/groups/:id/members", groupHandler.GetGroupMembers)
		authorized.POST("/groups/:id/members", groupHandler.AddMembers)
		authorized.DELETE("/groups/:id/members/:user_id", groupHandler.RemoveMember)
		authorized.POST("/groups/:id/messages", groupHandler.SendGroupMessage)
		authorized.PUT("/groups/:id", groupHandler.UpdateGroup)
		authorized.DELETE("/groups/:id", groupHandler.DisbandGroup)
```

### 验证Step 4

```bash
go build cmd/server/main.go
# 期望: 编译成功
```

---

## Step 5: 修改Group Model添加Type字段 (10分钟)

### 文件: `apps/backend/internal/model/group.go`

### 操作: 找到Group结构体

找到:
```go
type Group struct {
	ID          uint      `gorm:"primarykey" json:"id"`
	Name        string    `gorm:"not null;size:100" json:"name"`
	Avatar      string    `gorm:"size:500" json:"avatar"`
	OwnerID     uint      `gorm:"not null;index" json:"owner_id"`
	Description string    `gorm:"type:text" json:"description"`
```

在`OwnerID`之后添加:
```go
	Type        string    `gorm:"type:enum('normal','department');default:'normal'" json:"type"` // ✅ 新增
```

### 操作: 在文件末尾添加常量

```go
// GroupType 常量
const (
	GroupTypeNormal     = "normal"
	GroupTypeDepartment = "department"
)
```

### 验证Step 5

```bash
go build cmd/server/main.go
# 期望: 编译成功
```

---

## Step 6: 创建数据库迁移 (10分钟)

### 迁移文件1: 添加group.type字段

```bash
# 创建文件
apps/backend/migrations/012_add_group_type.up.sql
apps/backend/migrations/012_add_group_type.down.sql
```

```sql
-- 012_add_group_type.up.sql
ALTER TABLE `groups` 
ADD COLUMN `type` ENUM('normal', 'department') DEFAULT 'normal' COMMENT '群组类型'
AFTER `owner_id`;
```

```sql
-- 012_add_group_type.down.sql
ALTER TABLE `groups` 
DROP COLUMN `type`;
```

### 执行迁移

```bash
# 连接MySQL
mysql -u root -p

# 选择数据库
USE lanxin_im;

# 执行迁移
source D:/im-lanxin/apps/backend/migrations/012_add_group_type.up.sql;

# 验证
DESCRIBE groups;
# 期望: 看到type字段
```

---

## Step 7: 功能测试 (30分钟)

### 启动服务器

```bash
cd D:\im-lanxin\apps\backend
go run cmd/server/main.go
```

### 测试用例1: 创建群组

```bash
# 登录获取token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"identifier\":\"testuser1\",\"password\":\"password123\"}"

$TOKEN="..."

# 创建群组
curl -X POST http://localhost:8080/api/v1/groups \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"测试群组\",\"avatar\":\"\",\"member_ids\":[2,3,4]}"

# ✅ 期望返回:
# {
#   "code": 0,
#   "message": "success",
#   "data": {
#     "group": {
#       "id": 1,
#       "name": "测试群组",
#       "owner_id": 1,
#       "type": "normal",  ← ✅ 有type字段
#       "member_count": 4,
#       "status": "active"
#     }
#   }
# }
```

### 测试用例2: 获取群组信息

```bash
curl http://localhost:8080/api/v1/groups/1 \
  -H "Authorization: Bearer $TOKEN"

# ✅ 期望: 返回完整的群组信息
```

### 测试用例3: 获取群成员

```bash
curl http://localhost:8080/api/v1/groups/1/members \
  -H "Authorization: Bearer $TOKEN"

# ✅ 期望返回:
# {
#   "code": 0,
#   "message": "success",
#   "data": {
#     "members": [
#       {"user_id": 1, "role": "owner", ...},
#       {"user_id": 2, "role": "member", ...},
#       {"user_id": 3, "role": "member", ...},
#       {"user_id": 4, "role": "member", ...}
#     ]
#   }
# }
```

### 测试用例4: 发送群消息

```bash
curl -X POST http://localhost:8080/api/v1/groups/1/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"content\":\"大家好\",\"type\":\"text\"}"

# ✅ 期望: 消息发送成功
```

### 测试用例5: 添加群成员

```bash
curl -X POST http://localhost:8080/api/v1/groups/1/members \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"member_ids\":[5,6]}"

# ✅ 期望: 添加成功
# 验证: member_count应该增加到6
```

### 测试用例6: 移除群成员

```bash
curl -X DELETE http://localhost:8080/api/v1/groups/1/members/6 \
  -H "Authorization: Bearer $TOKEN"

# ✅ 期望: 移除成功
# 验证: member_count应该减少到5
```

---

## Step 8: 数据库验证 (5分钟)

```sql
-- 连接MySQL
USE lanxin_im;

-- 1. 检查群组表
SELECT * FROM groups;
-- 期望: 有1条记录,type='normal', member_count=4

-- 2. 检查群成员表
SELECT gm.*, u.username 
FROM group_members gm
LEFT JOIN users u ON gm.user_id = u.id
WHERE gm.group_id = 1;
-- 期望: 有4条记录(1个owner+3个member)

-- 3. 检查群消息(如果发送了)
SELECT * FROM messages WHERE group_id = 1;
-- 期望: 有消息记录
```

---

## Step 9: 提交代码 (5分钟)

```bash
git status

# 期望看到:
# new file:   apps/backend/internal/dao/group_dao.go
# new file:   apps/backend/internal/service/group_service.go
# new file:   apps/backend/internal/api/group.go
# modified:   apps/backend/cmd/server/main.go
# modified:   apps/backend/internal/model/group.go
# new file:   apps/backend/migrations/012_add_group_type.up.sql
# new file:   apps/backend/migrations/012_add_group_type.down.sql

git add apps/backend/internal/dao/group_dao.go
git add apps/backend/internal/service/group_service.go
git add apps/backend/internal/api/group.go
git add apps/backend/cmd/server/main.go
git add apps/backend/internal/model/group.go
git add apps/backend/migrations/012_add_group_type.up.sql
git add apps/backend/migrations/012_add_group_type.down.sql

git commit -m "feat: 实现完整的群聊功能

新增功能:
- GroupDAO: 群组数据访问层
- GroupMemberDAO: 群成员数据访问层
- GroupService: 群组业务逻辑层
- GroupHandler: 群组API层
- 8个群组相关路由

API列表:
- POST /groups - 创建群组
- GET /groups/:id - 获取群组信息
- GET /groups/:id/members - 获取群成员
- POST /groups/:id/members - 添加成员
- DELETE /groups/:id/members/:user_id - 移除成员
- POST /groups/:id/messages - 发送群消息
- PUT /groups/:id - 更新群信息
- DELETE /groups/:id - 解散群组

数据库变更:
- 添加groups.type字段

测试通过:
- 创建群组成功
- 获取群信息成功
- 发送群消息成功
- 成员管理正常"
```

---

# ✅ 阶段2验收

## 必须全部通过 (12/12)

```
[ ] 1. group_dao.go创建成功
[ ] 2. group_service.go创建成功
[ ] 3. group.go API创建成功
[ ] 4. main.go添加路由成功
[ ] 5. Group Model添加Type字段
[ ] 6. 数据库迁移执行成功
[ ] 7. 代码编译成功
[ ] 8. 服务器正常启动
[ ] 9. 创建群组API测试通过
[ ] 10. 获取群信息API测试通过
[ ] 11. 发送群消息API测试通过
[ ] 12. Git提交完成
```

---

# 📊 阶段2完成标志

```
✅ 群组CRUD功能完整
✅ 群成员管理完整
✅ 群消息发送正常
✅ 8个API全部可用
✅ 数据库结构完整
```

**如果本阶段全部通过,请继续**: `PHASE_3_ADD_FOREIGN_KEYS.md`  
**如果本阶段有任何失败,请停止并修复**

---

**文档版本**: 1.0  
**创建时间**: 2025-10-18  
**预计完成时间**: 2-3小时  
**实际完成时间**: ________  
**验收结果**: ⬜ 通过 / ⬜ 失败

