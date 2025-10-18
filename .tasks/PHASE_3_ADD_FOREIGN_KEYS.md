# 阶段3: 添加数据库外键约束
## 单一目标 - 数据完整性 - 严格测试

**阶段编号**: Phase 3 of 5  
**预计时间**: 30分钟  
**前置条件**: 阶段1和阶段2完成  
**成功标准**: 所有外键约束正确添加,数据完整性有保障

---

# 🎯 本阶段唯一目标

**添加**: messages表的conversation_id外键约束

**当前问题**:
```sql
-- messages表
conversation_id BIGINT UNSIGNED NOT NULL,
-- ❌ 没有外键约束!
-- 可以设置为不存在的会话ID
```

**修复后**:
```sql
-- messages表
conversation_id BIGINT UNSIGNED NOT NULL,
FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
-- ✅ 有外键约束
-- 只能设置存在的会话ID
```

---

# 📋 修复步骤

## Step 1: 创建迁移文件 (5分钟)

### 创建UP迁移

```bash
# 文件: apps/backend/migrations/013_add_conversation_fk_to_messages.up.sql
```

```sql
-- 添加外键约束: messages.conversation_id → conversations.id
ALTER TABLE `messages` 
ADD CONSTRAINT `fk_messages_conversation` 
  FOREIGN KEY (`conversation_id`) 
  REFERENCES `conversations`(`id`) 
  ON DELETE CASCADE
  ON UPDATE CASCADE;
```

### 创建DOWN迁移

```bash
# 文件: apps/backend/migrations/013_add_conversation_fk_to_messages.down.sql
```

```sql
-- 删除外键约束
ALTER TABLE `messages` 
DROP FOREIGN KEY `fk_messages_conversation`;
```

---

## Step 2: 执行迁移前的数据清理 (5分钟)

### 检查是否有脏数据

```sql
-- 连接MySQL
USE lanxin_im;

-- 查找conversation_id=0的消息
SELECT COUNT(*) FROM messages WHERE conversation_id = 0;

-- 如果有数据,需要清理
-- ⚠️ 警告: 这会删除所有conversation_id=0的消息
DELETE FROM messages WHERE conversation_id = 0;

-- 查找conversation_id指向不存在会话的消息
SELECT COUNT(*) 
FROM messages m
LEFT JOIN conversations c ON m.conversation_id = c.id
WHERE c.id IS NULL AND m.conversation_id != 0;

-- 如果有数据,需要清理
DELETE m FROM messages m
LEFT JOIN conversations c ON m.conversation_id = c.id
WHERE c.id IS NULL AND m.conversation_id != 0;
```

---

## Step 3: 执行迁移 (5分钟)

```bash
# 方式1: 使用mysql命令
mysql -u root -p lanxin_im < D:/im-lanxin/apps/backend/migrations/013_add_conversation_fk_to_messages.up.sql

# 方式2: 在MySQL客户端中
USE lanxin_im;
source D:/im-lanxin/apps/backend/migrations/013_add_conversation_fk_to_messages.up.sql;
```

### 验证迁移成功

```sql
-- 查看表结构
SHOW CREATE TABLE messages;

-- 期望看到:
-- CONSTRAINT `fk_messages_conversation` 
-- FOREIGN KEY (`conversation_id`) 
-- REFERENCES `conversations` (`id`) 
-- ON DELETE CASCADE ON UPDATE CASCADE
```

---

## Step 4: 测试外键约束 (10分钟)

### 测试用例1: 插入有效数据

```sql
-- 应该成功
INSERT INTO messages (
    conversation_id, sender_id, receiver_id, 
    content, type, status
) VALUES (
    1, 1, 2, 
    '测试消息', 'text', 'sent'
);
-- ✅ 期望: 插入成功 (conversation_id=1存在)
```

### 测试用例2: 插入无效数据

```sql
-- 应该失败
INSERT INTO messages (
    conversation_id, sender_id, receiver_id, 
    content, type, status
) VALUES (
    9999, 1, 2, 
    '测试消息', 'text', 'sent'
);
-- ❌ 期望: 报错 "Cannot add or update a child row: a foreign key constraint fails"
-- ✅ 这说明外键约束生效了!
```

### 测试用例3: 级联删除

```sql
-- 删除会话,检查消息是否级联删除
-- 先记录消息数量
SELECT COUNT(*) FROM messages WHERE conversation_id = 1;
-- 假设有5条

-- 删除会话
DELETE FROM conversations WHERE id = 1;

-- 检查消息是否被级联删除
SELECT COUNT(*) FROM messages WHERE conversation_id = 1;
-- ✅ 期望: 0 (所有消息都被级联删除了)

-- 恢复数据(用于后续测试)
-- (重新发送消息会自动创建会话)
```

---

## Step 5: 提交代码 (5分钟)

```bash
git status

# 期望看到:
# new file:   apps/backend/migrations/013_add_conversation_fk_to_messages.up.sql
# new file:   apps/backend/migrations/013_add_conversation_fk_to_messages.down.sql

git add apps/backend/migrations/013_add_conversation_fk_to_messages.up.sql
git add apps/backend/migrations/013_add_conversation_fk_to_messages.down.sql

git commit -m "feat: 添加messages表conversation_id外键约束

数据库变更:
- 添加外键约束: messages.conversation_id → conversations.id
- 级联删除: 删除会话时自动删除相关消息
- 级联更新: 更新会话ID时自动更新消息

测试通过:
- 外键约束生效
- 无法插入不存在的conversation_id
- 级联删除正常工作"
```

---

# ✅ 阶段3验收

## 必须全部通过 (8/8)

```
[ ] 1. 迁移文件创建成功(up+down)
[ ] 2. 脏数据清理完成
[ ] 3. 迁移执行成功
[ ] 4. SHOW CREATE TABLE显示外键
[ ] 5. 测试插入有效数据成功
[ ] 6. 测试插入无效数据失败(说明约束生效)
[ ] 7. 测试级联删除正常
[ ] 8. Git提交完成
```

---

# 📊 阶段3完成标志

```
✅ messages.conversation_id有外键约束
✅ 数据完整性得到保障
✅ 无法插入非法数据
✅ 级联删除自动处理
```

**如果本阶段全部通过,请继续**: `PHASE_4_OFFLINE_MESSAGE_QUEUE.md`  
**如果本阶段有任何失败,请停止并修复**

---

**文档版本**: 1.0  
**创建时间**: 2025-10-18  
**预计完成时间**: 30分钟  
**实际完成时间**: ________  
**验收结果**: ⬜ 通过 / ⬜ 失败

