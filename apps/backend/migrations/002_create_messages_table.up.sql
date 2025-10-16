CREATE TABLE IF NOT EXISTS messages (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
    sender_id BIGINT UNSIGNED NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
    content TEXT NOT NULL COMMENT '消息内容',
    type ENUM('text', 'image', 'voice', 'video', 'file') DEFAULT 'text' COMMENT '消息类型',
    file_url VARCHAR(500) COMMENT '文件URL（图片/语音/视频/文件）',
    file_size BIGINT COMMENT '文件大小（字节）',
    duration INT COMMENT '语音/视频时长（秒）',
    status ENUM('sent', 'delivered', 'read', 'recalled') DEFAULT 'sent' COMMENT '消息状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）',
    INDEX idx_conversation (conversation_id),
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted_at (deleted_at),
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

