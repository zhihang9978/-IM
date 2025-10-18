-- 恢复messages表的receiver_id为NOT NULL
-- 注意：执行此回滚前需确保所有receiver_id不为NULL
ALTER TABLE `messages` 
MODIFY COLUMN `receiver_id` BIGINT UNSIGNED NOT NULL COMMENT '接收者ID';

