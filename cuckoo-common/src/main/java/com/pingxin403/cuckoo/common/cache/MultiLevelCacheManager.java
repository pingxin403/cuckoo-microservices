package com.pingxin403.cuckoo.common.cache;

import java.time.Duration;

/**
 * 多级缓存管理器接口
 * 提供本地缓存（Caffeine）+ 分布式缓存（Redis）的两级缓存策略
 */
public interface MultiLevelCacheManager {
    
    /**
     * 获取缓存值
     * 查询顺序：本地缓存 -> Redis 缓存 -> 返回 null
     * 
     * @param key 缓存键
     * @param type 值类型
     * @return 缓存值，不存在返回 null
     */
    <T> T get(String key, Class<T> type);
    
    /**
     * 设置缓存值
     * 同时写入本地缓存和 Redis 缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间
     */
    void put(String key, Object value, Duration ttl);
    
    /**
     * 删除缓存
     * 同时删除本地缓存和 Redis 缓存
     * 并通过 Redis Pub/Sub 通知其他实例删除本地缓存
     * 
     * @param key 缓存键
     */
    void evict(String key);
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计
     */
    CacheStats getStats();
}
