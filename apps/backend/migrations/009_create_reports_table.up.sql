-- 创建举报消息表
-- 用途：用户举报的消息记录
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT UNSIGNED NOT NULL COMMENT '举报人ID',
    message_id BIGINT UNSIGNED NOT NULL COMMENT '被举报消息ID',
    reason VARCHAR(50) NOT NULL COMMENT '举报原因',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '处理状态: pending, reviewed, resolved',
    admin_note TEXT COMMENT '管理员备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_reporter_id (reporter_id),
    INDEX idx_message_id (message_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息举报表';

