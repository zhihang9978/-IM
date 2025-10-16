CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码（bcrypt加密）',
    avatar VARCHAR(500) COMMENT '头像URL',
    lanxin_id VARCHAR(50) UNIQUE NOT NULL COMMENT '蓝信号',
    role ENUM('user', 'admin') DEFAULT 'user' COMMENT '角色',
    status ENUM('active', 'banned', 'deleted') DEFAULT 'active' COMMENT '状态',
    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）',
    INDEX idx_lanxin_id (lanxin_id),
    INDEX idx_phone (phone),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

