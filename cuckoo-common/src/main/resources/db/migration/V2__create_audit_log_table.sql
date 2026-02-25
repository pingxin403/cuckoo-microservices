-- 审计日志表
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型：LOGIN, CREATE_ORDER, CANCEL_ORDER, UPDATE_PRODUCT, DELETE_DATA',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(100) COMMENT '用户名',
    resource_type VARCHAR(50) COMMENT '资源类型：ORDER, PRODUCT, USER',
    resource_id VARCHAR(100) COMMENT '资源ID',
    operation_result VARCHAR(20) NOT NULL COMMENT '操作结果：SUCCESS, FAILURE',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    request_params TEXT COMMENT '请求参数（JSON格式）',
    response_data TEXT COMMENT '响应数据（JSON格式）',
    error_message TEXT COMMENT '错误信息',
    trace_id VARCHAR(100) COMMENT '链路追踪ID',
    service_name VARCHAR(50) COMMENT '服务名称',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at),
    INDEX idx_trace_id (trace_id),
    INDEX idx_resource (resource_type, resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';
