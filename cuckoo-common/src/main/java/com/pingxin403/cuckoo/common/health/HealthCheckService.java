package com.pingxin403.cuckoo.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;

/**
 * 健康检查服务
 * 提供存活检查和就绪检查功能
 */
@Slf4j
@Component
public class HealthCheckService {
    
    @Autowired(required = false)
    private DataSource dataSource;
    
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired(required = false)
    private WarmupService warmupService;
    
    private volatile boolean isWarmedUp = false;
    
    /**
     * 存活检查：服务是否运行
     * 这个检查应该始终返回 UP，除非服务完全无法响应
     * 
     * @return 健康状态
     */
    public HealthStatus checkLiveness() {
        return HealthStatus.UP;
    }
    
    /**
     * 就绪检查：服务是否可以接收流量
     * 检查预热状态、数据库连接和 Redis 连接
     * 
     * @return 健康状态
     */
    public HealthStatus checkReadiness() {
        // 1. 检查预热状态
        if (!isWarmedUp) {
            log.debug("Service not ready: warmup not completed");
            return HealthStatus.DOWN;
        }
        
        // 2. 检查数据库连接（如果配置了数据源）
        if (dataSource != null && !checkDatabase()) {
            log.warn("Service not ready: database connection failed");
            return HealthStatus.DOWN;
        }
        
        // 3. 检查 Redis 连接（如果配置了 Redis）
        if (redisTemplate != null && !checkRedis()) {
            log.warn("Service not ready: Redis connection failed");
            return HealthStatus.DOWN;
        }
        
        return HealthStatus.UP;
    }
    
    /**
     * 检查数据库连接
     * 
     * @return true 如果数据库连接正常
     */
    private boolean checkDatabase() {
        if (dataSource == null) {
            return true;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(3);
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return false;
        }
    }
    
    /**
     * 检查 Redis 连接
     * 
     * @return true 如果 Redis 连接正常
     */
    private boolean checkRedis() {
        if (redisTemplate == null) {
            return true;
        }
        
        try {
            redisTemplate.opsForValue().get("health:check");
            return true;
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return false;
        }
    }
    
    /**
     * 服务预热
     * 在服务启动时执行，完成后才允许接收流量
     */
    @PostConstruct
    public void warmup() {
        log.info("Starting service warmup...");
        
        try {
            // 如果配置了 WarmupService，执行预热逻辑
            if (warmupService != null) {
                warmupService.performWarmup();
            } else {
                // 默认预热逻辑
                performDefaultWarmup();
            }
            
            isWarmedUp = true;
            log.info("Service warmup completed successfully");
            
        } catch (Exception e) {
            log.error("Service warmup failed, but continuing startup", e);
            // 即使预热失败也允许启动，避免服务无法启动
            isWarmedUp = true;
        }
    }
    
    /**
     * 默认预热逻辑
     */
    private void performDefaultWarmup() {
        // 1. 预热数据库连接池
        if (dataSource != null) {
            warmupDatabase();
        }
        
        // 2. 预热 Redis 连接池
        if (redisTemplate != null) {
            warmupRedis();
        }
    }
    
    /**
     * 预热数据库连接池
     */
    private void warmupDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            conn.prepareStatement("SELECT 1").execute();
            log.info("Database connection pool warmed up");
        } catch (Exception e) {
            log.error("Database warmup failed", e);
        }
    }
    
    /**
     * 预热 Redis 连接池
     */
    private void warmupRedis() {
        try {
            redisTemplate.opsForValue().set("warmup:test", "ok", Duration.ofSeconds(10));
            log.info("Redis connection pool warmed up");
        } catch (Exception e) {
            log.error("Redis warmup failed", e);
        }
    }
    
    /**
     * 获取预热状态
     * 
     * @return true 如果预热已完成
     */
    public boolean isWarmedUp() {
        return isWarmedUp;
    }
}
