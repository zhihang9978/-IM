-- ==========================================
-- 重置测试用户密码脚本
-- ==========================================
-- 用途: 为功能测试创建/重置测试用户
-- 执行: mysql -u root -p lanxin_im < RESET_TEST_USERS.sql
-- ==========================================

USE lanxin_im;

-- 清空现有测试用户（可选，谨慎使用）
-- DELETE FROM users WHERE lanxin_id LIKE 'lx0000%';

-- 创建/更新测试用户
-- 密码: password123 (bcrypt hash)
INSERT INTO users (username, password, lanxin_id, role, status, created_at, updated_at, deleted_at) VALUES
('testuser1', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5koSb3SXW1FKe', 'lx000001', 'user', 'active', NOW(), NOW(), NULL),
('testuser2', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5koSb3SXW1FKe', 'lx000002', 'user', 'active', NOW(), NOW(), NULL),
('testuser3', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5koSb3SXW1FKe', 'lx000003', 'user', 'active', NOW(), NOW(), NULL),
('testuser4', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5koSb3SXW1FKe', 'lx000004', 'user', 'active', NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE 
    password = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5koSb3SXW1FKe',
    status = 'active',
    deleted_at = NULL,
    updated_at = NOW();

-- 创建管理员账号
INSERT INTO users (username, password, lanxin_id, role, status, created_at, updated_at, deleted_at) VALUES
('admin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5koSb3SXW1FKe', 'lx999999', 'admin', 'active', NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE 
    password = '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5koSb3SXW1FKe',
    status = 'active',
    deleted_at = NULL,
    updated_at = NOW();

-- 验证结果
SELECT id, username, lanxin_id, role, status, created_at 
FROM users 
WHERE lanxin_id IN ('lx000001', 'lx000002', 'lx000003', 'lx000004', 'lx999999')
ORDER BY id;

-- ==========================================
-- 测试账号信息
-- ==========================================
-- 
-- 账号1: testuser1 / password123 (lx000001)
-- 账号2: testuser2 / password123 (lx000002)
-- 账号3: testuser3 / password123 (lx000003)
-- 账号4: testuser4 / password123 (lx000004)
-- 管理员: admin / password123 (lx999999)
-- 
-- ==========================================
-- 使用示例
-- ==========================================
-- 
-- 登录测试:
-- curl -X POST http://154.40.45.121:8080/api/v1/auth/login \
--   -H "Content-Type: application/json" \
--   -d '{"identifier":"testuser1","password":"password123"}'
-- 
-- 或使用蓝信号:
-- curl -X POST http://154.40.45.121:8080/api/v1/auth/login \
--   -H "Content-Type: application/json" \
--   -d '{"identifier":"lx000001","password":"password123"}'
-- 
-- ==========================================

