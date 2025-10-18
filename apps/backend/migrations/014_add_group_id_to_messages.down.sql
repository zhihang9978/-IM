-- 删除messages表的group_id字段
ALTER TABLE `messages` 
DROP INDEX `idx_group`,
DROP COLUMN `group_id`;

