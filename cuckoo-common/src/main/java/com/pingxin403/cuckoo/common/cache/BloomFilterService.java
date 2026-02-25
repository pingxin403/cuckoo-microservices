package com.pingxin403.cuckoo.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 布隆过滤器服务
 * 用于防止缓存穿透
 * 
 * 使用 Redis Bitmap 实现布隆过滤器
 * 误判率约为 1%（使用 3 个哈希函数，bitmap 大小为 10M）
 */
@Slf4j
@Component
public class BloomFilterService {
    
    private static final String BLOOM_FILTER_KEY = "bloom:filter:products";
    private static final int BIT_SIZE = 10_000_000; // 10M bits
    private static final int HASH_COUNT = 3; // 哈希函数数量
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public BloomFilterService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 添加元素到布隆过滤器
     * 
     * @param value 要添加的值
     */
    public void add(String value) {
        for (int i = 0; i < HASH_COUNT; i++) {
            long hash = hash(value, i);
            long bitIndex = Math.abs(hash % BIT_SIZE);
            redisTemplate.opsForValue().setBit(BLOOM_FILTER_KEY, bitIndex, true);
        }
        log.debug("Added to bloom filter: value={}", value);
    }
    
    /**
     * 检查元素是否可能存在
     * 
     * @param value 要检查的值
     * @return true 表示可能存在，false 表示一定不存在
     */
    public boolean mightContain(String value) {
        for (int i = 0; i < HASH_COUNT; i++) {
            long hash = hash(value, i);
            long bitIndex = Math.abs(hash % BIT_SIZE);
            Boolean bit = redisTemplate.opsForValue().getBit(BLOOM_FILTER_KEY, bitIndex);
            if (bit == null || !bit) {
                log.debug("Bloom filter check failed: value={}", value);
                return false;
            }
        }
        log.debug("Bloom filter check passed: value={}", value);
        return true;
    }
    
    /**
     * 计算哈希值
     * 
     * @param value 要哈希的值
     * @param seed 哈希种子
     * @return 哈希值
     */
    private long hash(String value, int seed) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest((value + seed).getBytes(StandardCharsets.UTF_8));
            
            // 取前 8 个字节转换为 long
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
    
    /**
     * 清空布隆过滤器
     */
    public void clear() {
        redisTemplate.delete(BLOOM_FILTER_KEY);
        log.info("Bloom filter cleared");
    }
}
