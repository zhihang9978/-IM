# 🧪 部署后完整功能测试

**服务器**: 154.40.45.121:8080  
**部署Commit**: 471cf03  
**测试目的**: 验证所有5个阶段的修复在生产环境正常工作

---

## 🔧 前置准备

### Step 0: 重置测试用户密码

```bash
# 在服务器执行
cd /var/www/im-lanxin
mysql -u root -p lanxin_im < RESET_TEST_USERS.sql

# 验证用户
mysql -u root -p -e "USE lanxin_im; SELECT id, username, lanxin_id FROM users WHERE lanxin_id LIKE 'lx0000%';"
```

**期望结果**:
- testuser1 (lx000001)
- testuser2 (lx000002)
- testuser3 (lx000003)
- testuser4 (lx000004)

---

## 📋 测试清单

### ✅ Test 1: 用户登录（基础功能）

```bash
# 测试testuser1登录
curl -X POST http://154.40.45.121:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"testuser1","password":"password123"}'

# 期望返回:
# {
#   "code": 0,
#   "message": "success",
#   "data": {
#     "token": "eyJhbGc...",
#     "user": {
#       "id": 1,
#       "username": "testuser1",
#       ...
#     }
#   }
# }

# 保存token用于后续测试
export TOKEN1="<从上面复制token>"
```

**验收标准**:
- [ ] 返回code=0
- [ ] 返回有效token
- [ ] 返回完整用户信息

---

### ✅ Test 2: 发送消息（阶段1修复验证）⭐

```bash
# testuser1 发送消息给 testuser2
curl -X POST http://154.40.45.121:8080/api/v1/messages \
  -H "Authorization: Bearer $TOKEN1" \
  -H "Content-Type: application/json" \
  -d '{"receiver_id":2,"content":"测试消息-验证会话创建","type":"text"}'

# 期望返回:
# {
#   "code": 0,
#   "data": {
#     "message": {
#       "id": 1,
#       "conversation_id": 1,  ← ⭐ 关键：不为0！
#       "sender_id": 1,
#       "receiver_id": 2,
#       "content": "测试消息-验证会话创建",
#       "status": "sent"
#     }
#   }
# }
```

**验收标准（阶段1修复）**:
- [ ] conversation_id **不为0**（修复前是0）
- [ ] 消息发送成功
- [ ] 返回完整消息信息

**如果conversation_id仍然是0**:
- ❌ 阶段1修复失败
- 检查conversation_dao.go是否正确部署
- 检查message_service.go是否调用GetOrCreateSingleConversation

---

### ✅ Test 3: 查询会话列表（阶段1验证）⭐

```bash
# testuser1 查询会话列表
curl http://154.40.45.121:8080/api/v1/conversations \
  -H "Authorization: Bearer $TOKEN1"

# 期望返回:
# {
#   "code": 0,
#   "data": {
#     "conversations": [
#       {
#         "id": 1,
#         "type": "single",
#         "user": {
#           "id": 2,
#           "username": "testuser2",
#           ...
#         },
#         "last_message": {
#           "id": 1,
#           "content": "测试消息-验证会话创建",  ← ⭐ 有内容
#           ...
#         },
#         "unread_count": 0
#       }
#     ]
#   }
# }
```

**验收标准（阶段1修复）**:
- [ ] conversations数组**不为空**（修复前为空）
- [ ] 包含刚才发送的消息
- [ ] last_message有内容

---

### ✅ Test 4: 创建群组（阶段2新功能验证）⭐⭐⭐

```bash
# testuser1 创建群组，添加testuser2和testuser3
curl -X POST http://154.40.45.121:8080/api/v1/groups \
  -H "Authorization: Bearer $TOKEN1" \
  -H "Content-Type: application/json" \
  -d '{"name":"测试群组","avatar":"","member_ids":[2,3]}'

# 期望返回:
# {
#   "code": 0,
#   "data": {
#     "group": {
#       "id": 1,
#       "name": "测试群组",
#       "owner_id": 1,
#       "type": "normal",  ← ⭐ 关键：新字段
#       "member_count": 3,
#       "status": "active",
#       ...
#     }
#   }
# }
```

**验收标准（阶段2新功能）**:
- [ ] 群组创建成功
- [ ] 返回type字段（修复前无此功能）
- [ ] member_count正确（3人）
- [ ] 返回群组ID

**如果返回404或500**:
- ❌ 阶段2部署失败
- 检查group_dao.go是否存在
- 检查main.go是否注册群组路由

---

### ✅ Test 5: 获取群组信息（阶段2验证）

```bash
# 获取刚创建的群组信息
curl http://154.40.45.121:8080/api/v1/groups/1 \
  -H "Authorization: Bearer $TOKEN1"

# 期望返回完整的群组信息
```

**验收标准**:
- [ ] 返回群组信息
- [ ] 包含owner和members信息

---

### ✅ Test 6: 发送群消息（阶段2验证）⭐⭐

```bash
# testuser1 在群组1中发送消息
curl -X POST http://154.40.45.121:8080/api/v1/groups/1/messages \
  -H "Authorization: Bearer $TOKEN1" \
  -H "Content-Type: application/json" \
  -d '{"content":"大家好，这是群消息测试","type":"text"}'

# 期望返回:
# {
#   "code": 0,
#   "data": {
#     "message": {
#       "id": 2,
#       "group_id": 1,  ← ⭐ 关键：群消息ID
#       "sender_id": 1,
#       "content": "大家好，这是群消息测试",
#       "status": "sent"
#     }
#   }
# }
```

**验收标准（阶段2新功能）**:
- [ ] 群消息发送成功
- [ ] 返回group_id字段（修复前无此功能）
- [ ] receiver_id为0或null（群消息特征）

---

### ✅ Test 7: 拉取离线消息（阶段4新功能验证）⭐⭐

```bash
# testuser2 拉取离线消息
# 先用testuser2登录
curl -X POST http://154.40.45.121:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"testuser2","password":"password123"}'

export TOKEN2="<token>"

# 拉取离线消息
curl http://154.40.45.121:8080/api/v1/messages/offline \
  -H "Authorization: Bearer $TOKEN2"

# 期望返回:
# {
#   "code": 0,
#   "data": {
#     "messages": [
#       {
#         "id": 1,
#         "content": "测试消息-验证会话创建",
#         ...
#       }
#     ],
#     "count": 1
#   }
# }
```

**验收标准（阶段4新功能）**:
- [ ] API能正常调用（修复前无此API）
- [ ] 返回离线消息列表
- [ ] count字段正确

---

### ✅ Test 8: 数据库完整性验证（阶段3验证）⭐

```sql
-- 连接MySQL
mysql -u root -p

USE lanxin_im;

-- 测试1: 验证外键约束存在
SHOW CREATE TABLE messages\G
-- 期望: 看到 CONSTRAINT `fk_messages_conversation`

-- 测试2: 验证外键约束生效（应该失败）
INSERT INTO messages (conversation_id, sender_id, receiver_id, content, type, status) 
VALUES (999999, 1, 2, 'test', 'text', 'sent');
-- 期望: ERROR 1452 - Cannot add or update a child row

-- 测试3: 验证数据关联正确
SELECT 
    c.id as conv_id,
    c.type,
    m.id as msg_id,
    m.conversation_id,
    m.content
FROM conversations c
LEFT JOIN messages m ON m.conversation_id = c.id
ORDER BY c.id DESC
LIMIT 5;
-- 期望: 消息正确关联到会话，无conversation_id=0

-- 测试4: 验证群组字段
DESCRIBE groups;
-- 期望: 有type字段

DESCRIBE messages;
-- 期望: 有group_id字段，receiver_id可空

EXIT;
```

**验收标准（阶段3数据完整性）**:
- [ ] 外键约束fk_messages_conversation存在
- [ ] 外键约束能拒绝非法数据
- [ ] 所有消息的conversation_id都有效
- [ ] groups表有type字段
- [ ] messages表有group_id字段

---

### ✅ Test 9: Redis离线消息队列验证（阶段4验证）

```bash
# 检查Redis
redis-cli -h 127.0.0.1 ping
# 期望: PONG

# 查看离线消息队列（可能为空）
redis-cli
> KEYS offline_msg:*
> LRANGE offline_msg:2 0 -1
> EXIT

# 如果有离线消息ID，说明队列正常工作
```

**验收标准（阶段4功能）**:
- [ ] Redis正常运行
- [ ] 离线消息队列可以创建
- [ ] 队列有过期时间（7天）

---

### ✅ Test 10: 完整端到端测试（综合验证）⭐⭐⭐

```bash
# 场景: 用户1给离线的用户2发消息

# 步骤1: 确保testuser2不在线（未连接WebSocket）

# 步骤2: testuser1发送消息
curl -X POST http://154.40.45.121:8080/api/v1/messages \
  -H "Authorization: Bearer $TOKEN1" \
  -H "Content-Type: application/json" \
  -d '{"receiver_id":2,"content":"离线消息测试","type":"text"}'

# 步骤3: 检查Redis队列
redis-cli
> LRANGE offline_msg:2 0 -1
# 期望: 返回消息ID列表，如 ["2"]

# 步骤4: testuser2上线并拉取
curl http://154.40.45.121:8080/api/v1/messages/offline \
  -H "Authorization: Bearer $TOKEN2"
# 期望: 返回离线消息

# 步骤5: 检查Redis队列已清空
redis-cli
> LRANGE offline_msg:2 0 -1
# 期望: (empty array) 或 (nil)
```

**验收标准（完整流程）**:
- [ ] 离线消息存入Redis
- [ ] 拉取API返回正确
- [ ] 拉取后队列清空
- [ ] 消息不丢失

---

## 📊 总验收标准

### 必须全部通过（15/15）

**基础功能（3项）**:
- [ ] 1. 用户能登录
- [ ] 2. 健康检查通过
- [ ] 3. 服务稳定运行

**阶段1修复验证（3项）**:
- [ ] 4. 发送消息conversation_id不为0
- [ ] 5. 会话列表不为空
- [ ] 6. 会话包含last_message

**阶段2新功能验证（3项）**:
- [ ] 7. 能创建群组
- [ ] 8. 群组有type字段
- [ ] 9. 能发送群消息

**阶段3数据完整性验证（2项）**:
- [ ] 10. 外键约束存在
- [ ] 11. 外键约束能拒绝非法数据

**阶段4新功能验证（3项）**:
- [ ] 12. 离线消息API可调用
- [ ] 13. 离线消息存入Redis
- [ ] 14. 拉取后队列清空

**综合验证（1项）**:
- [ ] 15. 完整端到端测试通过

---

## 🎯 测试通过标准

### 完全通过（15/15）
```
状态: ✅ 生产就绪
评分: 9.5/10
建议: 可以开放给用户使用
```

### 部分通过（12-14/15）
```
状态: ⚠️ 需要调优
评分: 8.0-9.0/10
建议: 修复未通过的测试项
```

### 未通过（<12/15）
```
状态: ❌ 需要修复
评分: <8.0/10
建议: 检查部署步骤，重新部署
```

---

## 📝 测试记录表

**执行人**: ____________  
**执行时间**: ____________  
**服务器**: 154.40.45.121

| 测试项 | 状态 | 备注 |
|--------|------|------|
| Test 1: 用户登录 | ⬜ | |
| Test 2: 发送消息 | ⬜ | conversation_id=? |
| Test 3: 会话列表 | ⬜ | |
| Test 4: 创建群组 | ⬜ | |
| Test 5: 群组信息 | ⬜ | |
| Test 6: 群消息 | ⬜ | |
| Test 7: 离线消息API | ⬜ | |
| Test 8: 数据库验证 | ⬜ | |
| Test 9: Redis验证 | ⬜ | |
| Test 10: 端到端测试 | ⬜ | |

**通过数量**: ____/10  
**验收结果**: ⬜ PASS / ⬜ FAIL

---

## 🎉 测试通过后

当所有测试通过后：

1. **更新项目状态**
   - 修改 PROJECT_FINAL_STATUS.txt
   - 标记为"生产就绪"

2. **通知团队**
   - 所有功能已验证
   - 可以开始用户测试

3. **监控运行**
   - 观察服务器日志
   - 监控资源使用
   - 收集用户反馈

---

**文档版本**: 1.0  
**创建时间**: 2025-10-18  
**适用环境**: 生产服务器 154.40.45.121

