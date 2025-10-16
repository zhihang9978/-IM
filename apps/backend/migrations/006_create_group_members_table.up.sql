CREATE TABLE IF NOT EXISTS group_members (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT UNSIGNED NOT NULL COMMENT '群组ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role ENUM('owner', 'admin', 'member') DEFAULT 'member' COMMENT '角色',
    nickname VARCHAR(50) COMMENT '群内昵称',
    muted BOOLEAN DEFAULT FALSE COMMENT '是否禁言',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_group (group_id),
    INDEX idx_user (user_id),
    INDEX idx_role (role),
    FOREIGN KEY (group_id) REFERENCES `groups`(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群成员表';

