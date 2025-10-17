-- 添加会话设置字段
-- 用途：支持免打扰、置顶等功能

ALTER TABLE conversations
ADD COLUMN is_muted BOOLEAN DEFAULT FALSE COMMENT '是否免打扰',
ADD COLUMN is_top BOOLEAN DEFAULT FALSE COMMENT '是否置顶',
ADD COLUMN is_starred BOOLEAN DEFAULT FALSE COMMENT '是否星标',
ADD COLUMN is_blocked BOOLEAN DEFAULT FALSE COMMENT '是否拉黑',
ADD INDEX idx_is_top (is_top),
ADD INDEX idx_is_muted (is_muted);

