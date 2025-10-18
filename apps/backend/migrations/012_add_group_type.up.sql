-- 添加groups表的type字段
-- 用于区分普通群组和部门群组
ALTER TABLE `groups` 
ADD COLUMN `type` ENUM('normal', 'department') DEFAULT 'normal' COMMENT '群组类型'
AFTER `owner_id`;

