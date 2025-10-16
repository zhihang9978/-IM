CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL COMMENT '操作类型',
    user_id BIGINT UNSIGNED COMMENT '操作用户ID',
    admin_id BIGINT UNSIGNED COMMENT '管理员ID（如果是管理员操作）',
    ip VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '客户端信息',
    details TEXT COMMENT 'JSON格式的详细信息',
    result VARCHAR(20) COMMENT '操作结果: success, failure',
    error_message VARCHAR(500) COMMENT '错误信息（如果失败）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_action (action),
    INDEX idx_user_id (user_id),
    INDEX idx_admin_id (admin_id),
    INDEX idx_result (result),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

