-- 删除全文搜索索引
ALTER TABLE messages DROP INDEX ft_content;
ALTER TABLE users DROP INDEX ft_user_search;

