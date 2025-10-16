CREATE TABLE IF NOT EXISTS contacts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    contact_id BIGINT UNSIGNED NOT NULL COMMENT '联系人ID',
    remark VARCHAR(50) COMMENT '备注名',
    tags VARCHAR(255) COMMENT '标签（逗号分隔）',
    status ENUM('normal', 'blocked') DEFAULT 'normal' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_contact (user_id, contact_id),
    INDEX idx_user (user_id),
    INDEX idx_contact (contact_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (contact_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人表';

