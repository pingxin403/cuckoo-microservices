-- ============================================================
-- Order Service 数据库表结构（order_db）
-- ============================================================

USE order_db;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(200),
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT'
        COMMENT 'PENDING_PAYMENT, PAID, CANCELLED',
    cancel_reason VARCHAR(200),
    payment_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 幂等性检查表（用于 Kafka 事件消费去重）
CREATE TABLE IF NOT EXISTS processed_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(50) NOT NULL UNIQUE,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Saga 分布式事务表结构
-- ============================================================

-- Saga 实例表
CREATE TABLE IF NOT EXISTS saga_instance (
    saga_id VARCHAR(64) PRIMARY KEY COMMENT 'Saga 实例 ID（UUID）',
    saga_type VARCHAR(100) NOT NULL COMMENT 'Saga 类型（如 ORDER_CREATION）',
    status VARCHAR(20) NOT NULL COMMENT 'RUNNING, COMPLETED, COMPENSATING, COMPENSATED, FAILED',
    current_step INT DEFAULT 0 COMMENT '当前执行到的步骤索引',
    context TEXT COMMENT 'Saga 上下文数据（JSON 格式）',
    execution_log TEXT COMMENT 'Saga 执行日志',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    timeout_at TIMESTAMP NOT NULL COMMENT '超时时间',
    INDEX idx_status (status),
    INDEX idx_timeout (timeout_at),
    INDEX idx_saga_type (saga_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Saga 实例表';

-- Saga 步骤执行记录表
CREATE TABLE IF NOT EXISTS saga_step_execution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    saga_id VARCHAR(64) NOT NULL COMMENT 'Saga 实例 ID',
    step_name VARCHAR(100) NOT NULL COMMENT '步骤名称',
    step_order INT NOT NULL COMMENT '步骤顺序',
    status VARCHAR(20) NOT NULL COMMENT 'PENDING, RUNNING, COMPLETED, FAILED, COMPENSATED',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    error_message TEXT NULL COMMENT '错误信息',
    FOREIGN KEY (saga_id) REFERENCES saga_instance(saga_id) ON DELETE CASCADE,
    INDEX idx_saga_id (saga_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Saga 步骤执行记录表';

-- ============================================================
-- CQRS 读写分离表结构
-- ============================================================

-- 订单写模型表（用于命令操作：创建、更新、删除）
CREATE TABLE IF NOT EXISTS order_write (
    order_id VARCHAR(64) PRIMARY KEY COMMENT '订单 ID（UUID）',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    product_name VARCHAR(200) COMMENT '商品名称',
    quantity INT NOT NULL COMMENT '购买数量',
    unit_price DECIMAL(10, 2) NOT NULL COMMENT '单价',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '总金额',
    status VARCHAR(20) NOT NULL COMMENT '订单状态：PENDING_PAYMENT, PAID, CANCELLED',
    cancel_reason VARCHAR(200) COMMENT '取消原因',
    payment_id BIGINT COMMENT '支付单 ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单写模型表';

-- 订单写模型明细表
CREATE TABLE IF NOT EXISTS order_item_write (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL COMMENT '订单 ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    product_name VARCHAR(200) COMMENT '商品名称',
    quantity INT NOT NULL COMMENT '购买数量',
    unit_price DECIMAL(10, 2) NOT NULL COMMENT '单价',
    subtotal DECIMAL(10, 2) NOT NULL COMMENT '小计',
    FOREIGN KEY (order_id) REFERENCES order_write(order_id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单写模型明细表';

-- 订单读模型表（优化查询的反范式设计）
CREATE TABLE IF NOT EXISTS order_read (
    order_id VARCHAR(64) PRIMARY KEY COMMENT '订单 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    user_name VARCHAR(100) COMMENT '用户名称（冗余字段）',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '总金额',
    status VARCHAR(20) NOT NULL COMMENT '订单状态',
    status_display VARCHAR(50) COMMENT '状态显示文本（中文）',
    item_count INT COMMENT '商品数量',
    product_names TEXT COMMENT '商品名称列表（逗号分隔）',
    sku_ids TEXT COMMENT 'SKU ID 列表（逗号分隔）',
    payment_id BIGINT COMMENT '支付单 ID',
    cancel_reason VARCHAR(200) COMMENT '取消原因',
    created_at TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id_created (user_id, created_at DESC) COMMENT '用户订单列表查询索引',
    INDEX idx_status (status) COMMENT '状态查询索引',
    INDEX idx_created_at (created_at DESC) COMMENT '时间排序索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单读模型表（反范式设计）';

-- 读模型同步状态表（用于追踪同步进度和失败重试）
CREATE TABLE IF NOT EXISTS order_read_sync_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL COMMENT '订单 ID',
    event_id VARCHAR(64) NOT NULL COMMENT '事件 ID',
    event_type VARCHAR(100) NOT NULL COMMENT '事件类型',
    sync_status VARCHAR(20) NOT NULL COMMENT '同步状态：PENDING, SUCCESS, FAILED',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    error_message TEXT COMMENT '错误信息',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_event_id (event_id),
    INDEX idx_sync_status (sync_status),
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='读模型同步状态表';
