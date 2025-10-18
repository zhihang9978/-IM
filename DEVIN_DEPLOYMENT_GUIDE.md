# 🚀 Devin服务器部署指南

**目标**: 将本地完成的所有修复部署到服务器  
**当前状态**: 服务器代码是旧版本，需要完整部署  
**预计时间**: 30-45分钟  
**风险等级**: ⚠️ 中等（涉及数据库迁移）

---

## 📋 部署前准备（必读）

### 当前情况说明

1. **本地仓库**: 已完成所有5个阶段的修复（commit: ee9b39c）
2. **远程仓库**: 已推送所有修改
3. **服务器**: 代码是旧版本，需要拉取更新
4. **数据库**: 缺少4个新迁移（012、013、014、015）

### 修复内容概览

- ✅ 阶段1: 会话自动创建（2个文件）
- ✅ 阶段2: 群聊功能（7个新文件 + 2个修改）
- ✅ 阶段3: 外键约束（2个迁移文件）
- ✅ 阶段4: 离线消息（2个文件修改）
- ✅ 阶段5: Android集成（3个文件修改）
- ✅ 补充: 数据库迁移（6个迁移文件）

---

## ⚠️ 部署注意事项

1. **数据备份**: 执行迁移前必须备份数据库
2. **停机时间**: 预计5-10分钟
3. **回滚准备**: 保留旧版本代码和数据库备份
4. **依赖检查**: 确保Redis已安装并运行

---

## 📝 执行步骤（严格按顺序）

### Step 1: 连接服务器并检查环境 (5分钟)

```bash
# SSH连接到服务器
ssh user@your-server-ip

# 检查当前工作目录
pwd
# 期望输出: /path/to/deployment/directory

# 进入项目目录（根据实际情况调整）
cd /var/www/im-lanxin
# 或
cd ~/im-lanxin

# 查看当前分支和提交
git branch
git log --oneline -5

# 期望看到:
# 当前在 master 分支
# 最新提交是旧版本（不是 ee9b39c）
```

**验证点**:
- [ ] 成功连接到服务器
- [ ] 找到项目目录
- [ ] 确认当前是旧版本代码

---

### Step 2: 停止后端服务 (2分钟)

```bash
# 方式1: 如果使用systemd
sudo systemctl stop lanxin-im
sudo systemctl status lanxin-im
# 期望: inactive (dead)

# 方式2: 如果使用pm2
pm2 stop lanxin-im
pm2 status

# 方式3: 如果使用screen/nohup
# 找到进程并kill
ps aux | grep "lanxin-im\|main.go"
# kill -9 <PID>

# 方式4: 如果在终端直接运行
# Ctrl+C 停止
```

**验证点**:
- [ ] 后端服务已停止
- [ ] 无相关进程在运行

---

### Step 3: 备份当前代码和数据库 (5分钟) ⭐⭐⭐

```bash
# 备份当前代码
cd /var/www  # 或项目父目录
cp -r im-lanxin im-lanxin.backup.$(date +%Y%m%d_%H%M%S)
# 或使用tar
tar -czf im-lanxin.backup.$(date +%Y%m%d_%H%M%S).tar.gz im-lanxin

# 备份数据库
mysqldump -u root -p lanxin_im > lanxin_im_backup_$(date +%Y%m%d_%H%M%S).sql

# 验证备份文件
ls -lh *backup* *.sql
# 期望: 看到备份文件，大小合理（非0）
```

**⚠️ 重要**: 
- 必须确认备份成功才能继续
- 备份文件应保存至少7天
- 如果部署失败，可以使用这些备份回滚

**验证点**:
- [ ] 代码备份文件存在且完整
- [ ] 数据库备份文件存在且大小>0
- [ ] 记录备份文件路径（用于可能的回滚）

---

### Step 4: 拉取最新代码 (3分钟)

```bash
# 进入项目目录
cd im-lanxin

# 检查当前状态
git status
# 如果有未提交的修改，先备份或提交

# 拉取最新代码
git fetch origin
git pull origin master

# 验证拉取结果
git log --oneline -5

# 期望看到最新的提交:
# ee9b39c docs: add comprehensive deployment checklist
# 47055d1 fix: add missing database migrations for group chat features
# 62ca5f9 feat(android): integrate group chat and offline messages (phase 5)
# 0894f59 feat: complete backend core fixes (phases 1-4)

# 检查新增的文件
ls -la apps/backend/internal/dao/group_dao.go
ls -la apps/backend/internal/service/group_service.go
ls -la apps/backend/internal/api/group.go
ls -la apps/backend/migrations/012_*.sql
ls -la apps/backend/migrations/013_*.sql
ls -la apps/backend/migrations/014_*.sql
ls -la apps/backend/migrations/015_*.sql

# 期望: 所有文件都存在
```

**验证点**:
- [ ] Git拉取成功，无冲突
- [ ] 最新提交是 ee9b39c
- [ ] 新增的7个后端文件存在
- [ ] 8个迁移文件（4个up + 4个down）存在

---

### Step 5: 更新Go依赖 (2分钟)

```bash
cd apps/backend

# 更新依赖
go mod tidy
go mod download

# 验证依赖
go mod verify
# 期望: all modules verified

# 检查是否能编译
go build -o test_compile cmd/server/main.go
ls -lh test_compile
# 期望: 生成可执行文件，大小>20MB

# 删除测试文件
rm test_compile
```

**验证点**:
- [ ] go mod tidy 执行成功
- [ ] 依赖验证通过
- [ ] 代码可以编译

---

### Step 6: 执行数据库迁移 (10分钟) ⭐⭐⭐

```bash
# 连接MySQL
mysql -u root -p
# 输入密码

# 在MySQL中执行以下命令:
```

```sql
-- 1. 选择数据库
USE lanxin_im;

-- 2. 检查当前表结构（迁移前）
DESCRIBE groups;
DESCRIBE messages;
-- 记录当前字段，用于对比

-- 3. 清理可能的脏数据（非常重要！）
-- 检查conversation_id=0的消息
SELECT COUNT(*) FROM messages WHERE conversation_id = 0;

-- 如果有，先删除（外键约束会拒绝这些记录）
DELETE FROM messages WHERE conversation_id = 0;

-- 检查孤立消息
SELECT COUNT(*) 
FROM messages m
LEFT JOIN conversations c ON m.conversation_id = c.id
WHERE c.id IS NULL;

-- 如果有，删除
DELETE m FROM messages m
LEFT JOIN conversations c ON m.conversation_id = c.id
WHERE c.id IS NULL;

-- 4. 执行迁移（严格按顺序）
-- 迁移012: 添加群组类型字段
SOURCE /var/www/im-lanxin/apps/backend/migrations/012_add_group_type.up.sql;
-- 期望: Query OK, 0 rows affected

-- 迁移013: 添加外键约束
SOURCE /var/www/im-lanxin/apps/backend/migrations/013_add_conversation_fk_to_messages.up.sql;
-- 期望: Query OK, X rows affected

-- 迁移014: 添加群消息支持
SOURCE /var/www/im-lanxin/apps/backend/migrations/014_add_group_id_to_messages.up.sql;
-- 期望: Query OK, 0 rows affected

-- 迁移015: receiver_id可空
SOURCE /var/www/im-lanxin/apps/backend/migrations/015_modify_receiver_id_nullable.up.sql;
-- 期望: Query OK, X rows affected

-- 5. 验证迁移结果
DESCRIBE groups;
-- 期望: 有 type 字段（ENUM('normal','department')）

DESCRIBE messages;
-- 期望: 
--   - 有 group_id 字段（BIGINT UNSIGNED NULL）
--   - receiver_id 是可空的（NULL允许）

SHOW CREATE TABLE messages\G
-- 期望: 看到 fk_messages_conversation 外键约束

-- 6. 测试外键约束是否生效
-- 应该失败（conversation_id不存在）
INSERT INTO messages (conversation_id, sender_id, receiver_id, content, type, status) 
VALUES (999999, 1, 2, 'test', 'text', 'sent');
-- 期望: ERROR 1452 - Cannot add or update a child row: a foreign key constraint fails
-- 这说明外键约束正常工作

-- 7. 退出MySQL
EXIT;
```

**验证点**:
- [ ] 脏数据已清理
- [ ] 4个迁移全部执行成功
- [ ] groups表有type字段
- [ ] messages表有group_id字段
- [ ] messages表的receiver_id可空
- [ ] 外键约束fk_messages_conversation存在
- [ ] 外键约束能拒绝非法数据

---

### Step 7: 检查Redis服务 (2分钟)

```bash
# 检查Redis是否运行
redis-cli ping
# 期望: PONG

# 如果Redis未运行，启动它
# Ubuntu/Debian
sudo systemctl start redis
sudo systemctl enable redis

# CentOS/RHEL
sudo systemctl start redis
sudo systemctl enable redis

# 验证Redis配置
redis-cli
> INFO server
> EXIT
```

**验证点**:
- [ ] Redis服务正在运行
- [ ] 能连接到Redis
- [ ] Redis版本 >= 5.0

---

### Step 8: 启动后端服务 (3分钟)

```bash
cd /var/www/im-lanxin/apps/backend

# 方式1: 使用systemd（推荐生产环境）
sudo systemctl start lanxin-im
sudo systemctl status lanxin-im
# 期望: active (running)

# 查看日志
sudo journalctl -u lanxin-im -f --lines 50

# 方式2: 使用pm2（Node.js进程管理）
pm2 start "go run cmd/server/main.go" --name lanxin-im
pm2 status
pm2 logs lanxin-im

# 方式3: 使用nohup（简单但不推荐）
nohup go run cmd/server/main.go > server.log 2>&1 &
tail -f server.log

# 方式4: 编译后运行（推荐）
go build -o lanxin-im cmd/server/main.go
chmod +x lanxin-im
nohup ./lanxin-im > server.log 2>&1 &
tail -f server.log
```

**期望看到的日志**:
```
Server starting on :8080
Server mode: release (or debug)
Domain: your-domain.com
MySQL connected successfully
Redis connected successfully
WebSocket Hub started
```

**验证点**:
- [ ] 服务启动成功，无panic
- [ ] MySQL连接成功
- [ ] Redis连接成功
- [ ] WebSocket Hub启动
- [ ] 监听端口8080

---

### Step 9: 功能验收测试 (10分钟) ⭐⭐⭐

```bash
# 在服务器或本地执行以下测试

# 测试1: 健康检查
curl http://localhost:8080/health
# 期望: {"status":"ok","message":"LanXin IM Server is running","online_users":0}

# 测试2: 用户登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"admin","password":"admin123"}'
# 期望: 返回token和用户信息

# 保存token（替换为实际返回的token）
TOKEN="eyJhbGc..."

# 测试3: 发送消息（测试会话自动创建）
curl -X POST http://localhost:8080/api/v1/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"receiver_id":2,"content":"测试消息","type":"text"}'
# 期望: 返回消息，conversation_id 不为 0

# 测试4: 查询会话列表
curl http://localhost:8080/api/v1/conversations \
  -H "Authorization: Bearer $TOKEN"
# 期望: 返回会话列表，包含刚才的消息

# 测试5: 创建群组（新功能）
curl -X POST http://localhost:8080/api/v1/groups \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"测试群组","avatar":"","member_ids":[2,3]}'
# 期望: 返回群组信息，包含 type="normal"

# 测试6: 拉取离线消息（新功能）
curl http://localhost:8080/api/v1/messages/offline \
  -H "Authorization: Bearer $TOKEN"
# 期望: 返回离线消息列表（可能为空）

# 测试7: 检查Redis队列
redis-cli
> KEYS offline_msg:*
> EXIT
# 期望: 显示离线消息队列（可能为空）
```

**验收标准**:
- [ ] 健康检查返回ok
- [ ] 用户能正常登录
- [ ] 发送消息conversation_id不为0
- [ ] 会话列表能正常显示
- [ ] 能创建群组
- [ ] 离线消息API正常响应
- [ ] 所有API返回code=0（成功）

---

### Step 10: 监控和日志检查 (3分钟)

```bash
# 检查服务运行状态
ps aux | grep lanxin-im

# 检查端口监听
netstat -tlnp | grep 8080
# 或
ss -tlnp | grep 8080
# 期望: 看到8080端口在监听

# 检查最近的错误日志
# systemd
sudo journalctl -u lanxin-im --since "10 minutes ago" | grep -i error

# 文件日志
tail -100 server.log | grep -i "error\|panic\|fatal"

# 检查资源使用
top -p $(pgrep -f lanxin-im)
# 期望: CPU和内存使用正常（CPU<50%, 内存<500MB）
```

**验证点**:
- [ ] 进程正常运行
- [ ] 端口8080正在监听
- [ ] 无严重错误日志
- [ ] 资源使用正常

---

## ✅ 部署完成检查清单

完成所有步骤后，确认以下所有项目：

### 代码层面
- [ ] Git拉取成功，代码版本是 ee9b39c
- [ ] 新增的7个Go文件存在
- [ ] Go依赖更新成功
- [ ] 代码能编译通过

### 数据库层面
- [ ] 数据库已备份
- [ ] 4个迁移全部执行成功
- [ ] groups表有type字段
- [ ] messages表有group_id字段
- [ ] messages表的receiver_id可空
- [ ] 外键约束正常工作

### 服务层面
- [ ] 后端服务正常启动
- [ ] MySQL连接成功
- [ ] Redis连接成功
- [ ] 无panic或fatal错误
- [ ] 端口8080正在监听

### 功能层面
- [ ] 健康检查通过
- [ ] 用户登录正常
- [ ] 发送消息conversation_id不为0
- [ ] 会话列表正常
- [ ] 能创建群组
- [ ] 离线消息API正常

---

## 🔧 常见问题处理

### 问题1: Git拉取失败
```bash
# 检查网络
ping github.com

# 检查Git配置
git remote -v

# 强制拉取（谨慎使用）
git fetch --all
git reset --hard origin/master
```

### 问题2: 迁移执行失败
```bash
# 查看MySQL错误
SHOW ERRORS;

# 回滚迁移（如果需要）
SOURCE /var/www/im-lanxin/apps/backend/migrations/015_modify_receiver_id_nullable.down.sql;
SOURCE /var/www/im-lanxin/apps/backend/migrations/014_add_group_id_to_messages.down.sql;
SOURCE /var/www/im-lanxin/apps/backend/migrations/013_add_conversation_fk_to_messages.down.sql;
SOURCE /var/www/im-lanxin/apps/backend/migrations/012_add_group_type.down.sql;

# 恢复数据库备份
mysql -u root -p lanxin_im < lanxin_im_backup_YYYYMMDD_HHMMSS.sql
```

### 问题3: 编译失败
```bash
# 清理并重新下载依赖
go clean -modcache
go mod download
go mod verify

# 检查Go版本
go version
# 期望: go version go1.21 或更高
```

### 问题4: 服务启动失败
```bash
# 检查配置文件
cat apps/backend/config/config.yaml

# 检查端口占用
lsof -i :8080

# 直接运行查看详细错误
cd apps/backend
go run cmd/server/main.go
```

### 问题5: Redis连接失败
```bash
# 检查Redis服务
systemctl status redis

# 检查Redis配置
cat /etc/redis/redis.conf | grep bind
cat /etc/redis/redis.conf | grep port

# 测试连接
redis-cli -h 127.0.0.1 -p 6379 ping
```

---

## 🔄 回滚方案（如果部署失败）

```bash
# 1. 停止新服务
sudo systemctl stop lanxin-im

# 2. 恢复旧代码
cd /var/www
rm -rf im-lanxin
mv im-lanxin.backup.YYYYMMDD_HHMMSS im-lanxin

# 3. 恢复数据库
mysql -u root -p lanxin_im < lanxin_im_backup_YYYYMMDD_HHMMSS.sql

# 4. 启动旧服务
cd im-lanxin/apps/backend
sudo systemctl start lanxin-im

# 5. 验证
curl http://localhost:8080/health
```

---

## 📞 需要帮助？

如果遇到问题：

1. **保存所有错误日志**
   ```bash
   sudo journalctl -u lanxin-im > error.log
   mysql -u root -p -e "SHOW ERRORS" > mysql_errors.log
   ```

2. **不要执行回滚**，等待支持

3. **提供信息**：
   - 哪个步骤失败
   - 完整的错误信息
   - 系统环境（OS版本、Go版本、MySQL版本）

---

## 🎯 部署成功标志

当所有验收标准通过后：

- ✅ 服务器代码版本：ee9b39c
- ✅ 功能完整性：9.5/10
- ✅ 数据库结构：完整
- ✅ 所有API：正常工作

**状态**: 🎉 生产环境部署成功！

---

**文档版本**: 1.0  
**创建时间**: 2025-10-18  
**适用人员**: Devin（服务器部署）  
**预计执行时间**: 30-45分钟

