-- 添加全文搜索索引
-- 用途：优化消息搜索性能

-- 为messages表的content字段添加全文索引
ALTER TABLE messages ADD FULLTEXT INDEX ft_content (content) WITH PARSER ngram;

-- 注意：
-- 1. ngram解析器支持中文分词（最小2字符）
-- 2. 使用MATCH...AGAINST语法可大幅提升搜索性能
-- 3. 适用于MySQL 5.7+或MariaDB 10.0+

-- 为users表添加复合全文索引（用于用户搜索）
ALTER TABLE users ADD FULLTEXT INDEX ft_user_search (username, lanxin_id) WITH PARSER ngram;

-- 示例查询（优化后）：
-- SELECT * FROM messages 
-- WHERE MATCH(content) AGAINST('关键词' IN BOOLEAN MODE)
-- AND (sender_id = ? OR receiver_id = ?);

