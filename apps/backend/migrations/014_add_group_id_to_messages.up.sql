-- 添加messages表的group_id字段
-- 用于支持群聊消息
ALTER TABLE `messages` 
ADD COLUMN `group_id` BIGINT UNSIGNED NULL COMMENT '群组ID（群消息）'
AFTER `receiver_id`,
ADD INDEX `idx_group` (`group_id`);

-- 注意: 暂不添加外键约束，因为可能需要先迁移现有数据
-- 可以在后续迁移中添加: FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE

