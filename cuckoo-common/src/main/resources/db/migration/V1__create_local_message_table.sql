-- 创建本地消息表
-- 用于保证事件发布的可靠性，避免消息丢失

CREATE TABLE IF NOT EXISTS local_message (
    message_id VARCHAR(64) PRIMARY KEY COMMENT '消息ID（UUID）',
    event_type VARCHAR(100) NOT NULL COMMENT '事件类型',
    payload TEXT NOT NULL COMMENT '消息内容（JSON格式）',
    status VARCHAR(20) NOT NULL COMMENT '消息状态：PENDING-待发送, SENT-已发送, FAILED-发送失败',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    sent_at TIMESTAMP NULL COMMENT '发送成功时间',
    error_message TEXT NULL COMMENT '错误信息',
    INDEX idx_status_created (status, created_at) COMMENT '状态和创建时间索引，用于查询待发送消息'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表';
