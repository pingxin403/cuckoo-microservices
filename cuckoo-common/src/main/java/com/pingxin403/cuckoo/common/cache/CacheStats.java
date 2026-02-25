package com.pingxin403.cuckoo.common.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 缓存统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CacheStats {
    
    /**
     * 本地缓存命中次数
     */
    private long localHits;
    
    /**
     * Redis 缓存命中次数
     */
    private long redisHits;
    
    /**
     * 缓存未命中次数
     */
    private long misses;
    
    /**
     * 本地缓存大小
     */
    private long localCacheSize;
    
    /**
     * 本地缓存命中率
     */
    public double getLocalHitRate() {
        long total = localHits + redisHits + misses;
        return total == 0 ? 0.0 : (double) localHits / total;
    }
    
    /**
     * Redis 缓存命中率
     */
    public double getRedisHitRate() {
        long total = localHits + redisHits + misses;
        return total == 0 ? 0.0 : (double) redisHits / total;
    }
    
    /**
     * 总命中率
     */
    public double getTotalHitRate() {
        long total = localHits + redisHits + misses;
        return total == 0 ? 0.0 : (double) (localHits + redisHits) / total;
    }
}
