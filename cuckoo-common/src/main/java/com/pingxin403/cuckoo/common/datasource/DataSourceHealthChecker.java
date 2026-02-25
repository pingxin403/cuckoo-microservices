package com.pingxin403.cuckoo.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源健康检查器
 * 定期检查从库健康状态，记录不可用的从库
 * 
 * @author pingxin403
 */
@Component
public class DataSourceHealthChecker {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceHealthChecker.class);
    
    /**
     * 数据源健康状态缓存
     * key: 数据源名称, value: 是否健康
     */
    private final Map<String, Boolean> healthStatus = new ConcurrentHashMap<>();
    
    /**
     * 连续失败次数
     */
    private final Map<String, Integer> failureCount = new ConcurrentHashMap<>();
    
    /**
     * 失败阈值，超过此次数认为数据源不可用
     */
    private static final int FAILURE_THRESHOLD = 3;
    
    /**
     * 健康检查超时时间（毫秒）
     */
    private static final int HEALTH_CHECK_TIMEOUT = 3000;
    
    /**
     * 定期健康检查（每30秒）
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void checkHealth() {
        log.debug("开始数据源健康检查");
        // 健康检查逻辑将在具体服务中实现
        // 这里提供基础框架
    }
    
    /**
     * 检查单个数据源健康状态
     * 
     * @param dataSource 数据源
     * @param name 数据源名称
     * @return true 如果健康
     */
    public boolean checkDataSourceHealth(DataSource dataSource, String name) {
        try (Connection connection = dataSource.getConnection()) {
            if (connection != null && !connection.isClosed()) {
                // 执行简单查询测试连接
                boolean valid = connection.isValid(HEALTH_CHECK_TIMEOUT / 1000);
                
                if (valid) {
                    markHealthy(name);
                    return true;
                } else {
                    markUnhealthy(name);
                    return false;
                }
            } else {
                markUnhealthy(name);
                return false;
            }
        } catch (SQLException e) {
            log.warn("数据源 {} 健康检查失败: {}", name, e.getMessage());
            markUnhealthy(name);
            return false;
        }
    }
    
    /**
     * 标记数据源为健康
     */
    private void markHealthy(String name) {
        Boolean previousStatus = healthStatus.put(name, true);
        failureCount.put(name, 0);
        
        if (previousStatus != null && !previousStatus) {
            log.info("数据源 {} 恢复健康", name);
        }
    }
    
    /**
     * 标记数据源为不健康
     */
    private void markUnhealthy(String name) {
        int count = failureCount.getOrDefault(name, 0) + 1;
        failureCount.put(name, count);
        
        if (count >= FAILURE_THRESHOLD) {
            Boolean previousStatus = healthStatus.put(name, false);
            
            if (previousStatus == null || previousStatus) {
                log.error("数据源 {} 标记为不可用，连续失败次数: {}", name, count);
            }
        }
    }
    
    /**
     * 检查数据源是否健康
     * 
     * @param name 数据源名称
     * @return true 如果健康
     */
    public boolean isHealthy(String name) {
        return healthStatus.getOrDefault(name, true);
    }
    
    /**
     * 获取失败次数
     */
    public int getFailureCount(String name) {
        return failureCount.getOrDefault(name, 0);
    }
    
    /**
     * 重置健康状态
     */
    public void resetHealth(String name) {
        healthStatus.remove(name);
        failureCount.remove(name);
        log.info("重置数据源 {} 的健康状态", name);
    }
}
