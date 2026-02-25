package com.pingxin403.cuckoo.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 多级缓存管理器实现
 * L1: 本地缓存（Caffeine）- 5分钟过期，最大10000条
 * L2: 分布式缓存（Redis）- 1小时过期
 */
@Slf4j
@Component
public class MultiLevelCacheManagerImpl implements MultiLevelCacheManager {
    
    private static final String CACHE_EVICT_CHANNEL = "cache:evict";
    private static final int LOCAL_CACHE_MAX_SIZE = 10000;
    private static final int LOCAL_CACHE_TTL_MINUTES = 5;
    
    // L1: 本地缓存（Caffeine）
    private final Cache<String, Object> localCache;
    
    // L2: 分布式缓存（Redis）
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Redis 消息监听容器
    private final RedisMessageListenerContainer messageListenerContainer;
    
    // 缓存统计
    private final AtomicLong localHits = new AtomicLong(0);
    private final AtomicLong redisHits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    
    // Micrometer 指标
    private final Counter localHitCounter;
    private final Counter redisHitCounter;
    private final Counter missCounter;
    
    public MultiLevelCacheManagerImpl(
            RedisTemplate<String, Object> redisTemplate,
            RedisMessageListenerContainer messageListenerContainer,
            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.messageListenerContainer = messageListenerContainer;
        
        // 初始化本地缓存
        this.localCache = Caffeine.newBuilder()
                .maximumSize(LOCAL_CACHE_MAX_SIZE)
                .expireAfterWrite(LOCAL_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
                .recordStats()
                .build();
        
        // 初始化 Micrometer 指标
        this.localHitCounter = Counter.builder("cache.hits")
                .tag("level", "local")
                .description("Local cache hit count")
                .register(meterRegistry);
        
        this.redisHitCounter = Counter.builder("cache.hits")
                .tag("level", "redis")
                .description("Redis cache hit count")
                .register(meterRegistry);
        
        this.missCounter = Counter.builder("cache.misses")
                .description("Cache miss count")
                .register(meterRegistry);
        
        log.info("MultiLevelCacheManager initialized: maxSize={}, ttl={}min", 
                LOCAL_CACHE_MAX_SIZE, LOCAL_CACHE_TTL_MINUTES);
    }
    
    @PostConstruct
    public void init() {
        // 订阅缓存失效通知
        messageListenerContainer.addMessageListener(
                (message, pattern) -> {
                    String key = new String(message.getBody());
                    log.debug("Received cache eviction notification: key={}", key);
                    localCache.invalidate(key);
                },
                new ChannelTopic(CACHE_EVICT_CHANNEL)
        );
        log.info("Subscribed to cache eviction channel: {}", CACHE_EVICT_CHANNEL);
    }
    
    @Override
    public <T> T get(String key, Class<T> type) {
        // 1. 先查本地缓存
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            log.debug("Local cache hit: key={}", key);
            localHits.incrementAndGet();
            localHitCounter.increment();
            return type.cast(value);
        }
        
        // 2. 查 Redis 缓存
        value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            log.debug("Redis cache hit: key={}", key);
            redisHits.incrementAndGet();
            redisHitCounter.increment();
            
            // 回填本地缓存
            localCache.put(key, value);
            return type.cast(value);
        }
        
        // 3. 缓存未命中
        log.debug("Cache miss: key={}", key);
        misses.incrementAndGet();
        missCounter.increment();
        return null;
    }
    
    @Override
    public void put(String key, Object value, Duration ttl) {
        // 同时写入两级缓存
        localCache.put(key, value);
        redisTemplate.opsForValue().set(key, value, ttl);
        log.debug("Cache updated: key={}, ttl={}", key, ttl);
    }
    
    @Override
    public void evict(String key) {
        // 同时删除两级缓存
        localCache.invalidate(key);
        redisTemplate.delete(key);
        
        // 发布缓存失效消息，通知其他实例
        redisTemplate.convertAndSend(CACHE_EVICT_CHANNEL, key);
        log.debug("Cache evicted and notification sent: key={}", key);
    }
    
    @Override
    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = localCache.stats();
        
        return CacheStats.builder()
                .localHits(localHits.get())
                .redisHits(redisHits.get())
                .misses(misses.get())
                .localCacheSize(localCache.estimatedSize())
                .build();
    }
}
