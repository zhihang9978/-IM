-- 删除会话设置字段
ALTER TABLE conversations
DROP COLUMN is_muted,
DROP COLUMN is_top,
DROP COLUMN is_starred,
DROP COLUMN is_blocked;

