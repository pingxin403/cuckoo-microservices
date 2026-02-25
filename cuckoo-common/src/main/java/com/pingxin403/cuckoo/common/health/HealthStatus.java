package com.pingxin403.cuckoo.common.health;

/**
 * 健康状态枚举
 */
public enum HealthStatus {
    /**
     * 服务健康，可以接收流量
     */
    UP,
    
    /**
     * 服务不健康，不应接收流量
     */
    DOWN
}
