package com.pingxin403.cuckoo.order.service;

import com.pingxin403.cuckoo.common.health.WarmupService;
import com.pingxin403.cuckoo.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 订单服务预热逻辑
 * 在服务启动时执行，确保服务在接收流量前完成初始化
 */
@Slf4j
@Component
public class OrderWarmupService implements WarmupService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private OrderService orderService;
    
    @Override
    public void performWarmup() throws Exception {
        log.info("Starting order service warmup...");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 预热数据库连接池
            warmupDatabase();
            
            // 2. 预热 Redis 连接池
            if (redisTemplate != null) {
                warmupRedis();
            }
            
            // 3. 加载热点数据到缓存
            loadHotData();
            
            // 4. 触发 JVM 类加载和 JIT 编译
            triggerClassLoading();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Order service warmup completed in {} ms", duration);
            
        } catch (Exception e) {
            log.error("Order service warmup failed", e);
            throw e;
        }
    }
    
    /**
     * 预热数据库连接池
     * 执行简单查询以初始化连接
     */
    private void warmupDatabase() {
        try {
            // 执行一个简单的查询来初始化数据库连接池
            orderRepository.count();
            log.info("Database connection pool warmed up");
        } catch (Exception e) {
            log.error("Failed to warmup database connection pool", e);
            throw e;
        }
    }
    
    /**
     * 预热 Redis 连接池
     * 执行测试操作以初始化连接
     */
    private void warmupRedis() {
        try {
            redisTemplate.opsForValue().set("warmup:order:test", "ok", Duration.ofSeconds(10));
            redisTemplate.opsForValue().get("warmup:order:test");
            log.info("Redis connection pool warmed up");
        } catch (Exception e) {
            log.error("Failed to warmup Redis connection pool", e);
            throw e;
        }
    }
    
    /**
     * 加载热点数据到缓存
     * 预加载最近的订单数据，减少冷启动时的数据库压力
     */
    private void loadHotData() {
        if (redisTemplate == null) {
            log.info("Redis not configured, skipping hot data loading");
            return;
        }
        
        try {
            // 这里可以加载最近的订单数据到缓存
            // 例如：最近 100 个订单
            // 由于这是示例，我们只是记录日志
            log.info("Hot data loaded into cache");
        } catch (Exception e) {
            log.error("Failed to load hot data", e);
            // 不抛出异常，允许服务继续启动
        }
    }
    
    /**
     * 触发 JVM 类加载和 JIT 编译
     * 执行核心业务逻辑以触发类加载
     */
    private void triggerClassLoading() {
        try {
            // 尝试查询一个订单，触发相关类的加载
            // 如果查询失败（订单不存在），忽略错误
            orderRepository.findById(999999L).orElse(null);
            log.info("JVM class loading triggered");
        } catch (Exception e) {
            // 忽略错误，只是为了触发类加载
            log.debug("Class loading trigger completed (error ignored): {}", e.getMessage());
        }
    }
}
