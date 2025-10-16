CREATE TABLE IF NOT EXISTS `groups` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '群组名称',
    avatar VARCHAR(500) COMMENT '群头像URL',
    owner_id BIGINT UNSIGNED NOT NULL COMMENT '群主ID',
    description TEXT COMMENT '群组描述',
    member_count INT DEFAULT 0 COMMENT '成员数量',
    max_members INT DEFAULT 500 COMMENT '最大成员数',
    status ENUM('active', 'disbanded') DEFAULT 'active' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_owner (owner_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

