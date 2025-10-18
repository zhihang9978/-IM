-- 修改messages表的receiver_id为可空
-- 群消息时receiver_id为NULL
ALTER TABLE `messages` 
MODIFY COLUMN `receiver_id` BIGINT UNSIGNED NULL COMMENT '接收者ID（单聊时使用，群聊时为NULL）';

