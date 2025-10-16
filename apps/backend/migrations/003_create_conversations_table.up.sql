CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type ENUM('single', 'group') DEFAULT 'single' COMMENT '会话类型',
    user1_id BIGINT UNSIGNED COMMENT '用户1 ID（单聊）',
    user2_id BIGINT UNSIGNED COMMENT '用户2 ID（单聊）',
    group_id BIGINT UNSIGNED COMMENT '群组ID（群聊）',
    last_message_id BIGINT UNSIGNED COMMENT '最后一条消息ID',
    last_message_at TIMESTAMP NULL COMMENT '最后消息时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_type (type),
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id),
    INDEX idx_group (group_id),
    INDEX idx_last_message_at (last_message_at),
    UNIQUE KEY uk_single_chat (user1_id, user2_id),
    FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

