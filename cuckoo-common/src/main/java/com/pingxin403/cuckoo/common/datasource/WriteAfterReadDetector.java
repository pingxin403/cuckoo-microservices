package com.pingxin403.cuckoo.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 写后读检测器
 * 检测同一请求上下文中的写后读场景，确保读取到最新数据
 * 
 * 工作原理：
 * 1. 写操作后，记录写入的资源ID和时间戳
 * 2. 读操作前，检查是否刚刚写入过该资源
 * 3. 如果是写后读场景，强制路由到主库
 * 
 * @author pingxin403
 */
@Component
public class WriteAfterReadDetector {
    
    private static final Logger log = LoggerFactory.getLogger(WriteAfterReadDetector.class);
    
    /**
     * 写操作记录
     * key: 线程ID, value: 写入的资源信息
     */
    private final ThreadLocal<WriteContext> writeContext = new ThreadLocal<>();
    
    /**
     * 全局写操作缓存（用于跨线程检测）
     * key: 资源ID, value: 写入时间戳
     */
    private final Map<String, Long> globalWriteCache = new ConcurrentHashMap<>();
    
    /**
     * 写后读时间窗口（毫秒）
     * 在此时间窗口内的读操作将强制路由到主库
     */
    private static final long WRITE_AFTER_READ_WINDOW = 10000; // 10秒
    
    /**
     * 写上下文
     */
    private static class WriteContext {
        private final Map<String, Long> writes = new ConcurrentHashMap<>();
        
        public void recordWrite(String resourceId) {
            writes.put(resourceId, System.currentTimeMillis());
        }
        
        public boolean hasRecentWrite(String resourceId, long windowMs) {
            Long writeTime = writes.get(resourceId);
            if (writeTime == null) {
                return false;
            }
            return System.currentTimeMillis() - writeTime < windowMs;
        }
        
        public void clear() {
            writes.clear();
        }
    }
    
    /**
     * 记录写操作
     * 
     * @param resourceType 资源类型（如 "order", "product"）
     * @param resourceId 资源ID
     */
    public void recordWrite(String resourceType, String resourceId) {
        String key = buildKey(resourceType, resourceId);
        
        // 记录到线程本地上下文
        WriteContext context = writeContext.get();
        if (context == null) {
            context = new WriteContext();
            writeContext.set(context);
        }
        context.recordWrite(key);
        
        // 记录到全局缓存
        globalWriteCache.put(key, System.currentTimeMillis());
        
        log.debug("记录写操作: {}", key);
    }
    
    /**
     * 检查是否为写后读场景
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     * @return true 如果是写后读场景
     */
    public boolean isWriteAfterRead(String resourceType, String resourceId) {
        String key = buildKey(resourceType, resourceId);
        
        // 1. 检查线程本地上下文（同一请求）
        WriteContext context = writeContext.get();
        if (context != null && context.hasRecentWrite(key, WRITE_AFTER_READ_WINDOW)) {
            log.debug("检测到写后读场景（线程本地）: {}", key);
            return true;
        }
        
        // 2. 检查全局缓存（跨请求，但在时间窗口内）
        Long writeTime = globalWriteCache.get(key);
        if (writeTime != null) {
            long elapsed = System.currentTimeMillis() - writeTime;
            if (elapsed < WRITE_AFTER_READ_WINDOW) {
                log.debug("检测到写后读场景（全局缓存）: {}, 距离写入 {}ms", key, elapsed);
                return true;
            } else {
                // 清理过期的写记录
                globalWriteCache.remove(key);
            }
        }
        
        return false;
    }
    
    /**
     * 清除当前线程的写上下文
     * 应在请求结束时调用
     */
    public void clearContext() {
        WriteContext context = writeContext.get();
        if (context != null) {
            context.clear();
        }
        writeContext.remove();
        log.debug("清除写后读上下文");
    }
    
    /**
     * 清理过期的全局写记录
     * 定期调用以释放内存
     */
    public void cleanupExpiredWrites() {
        long now = System.currentTimeMillis();
        globalWriteCache.entrySet().removeIf(entry -> 
            now - entry.getValue() > WRITE_AFTER_READ_WINDOW
        );
        log.debug("清理过期写记录，剩余: {}", globalWriteCache.size());
    }
    
    /**
     * 构建缓存键
     */
    private String buildKey(String resourceType, String resourceId) {
        return resourceType + ":" + resourceId;
    }
    
    /**
     * 获取全局写缓存大小（用于监控）
     */
    public int getGlobalWriteCacheSize() {
        return globalWriteCache.size();
    }
}
