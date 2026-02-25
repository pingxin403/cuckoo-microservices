package com.pingxin403.cuckoo.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据源清理调度器
 * 定期清理过期的写后读记录
 * 
 * @author pingxin403
 */
@Component
public class DataSourceCleanupScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceCleanupScheduler.class);
    
    @Autowired(required = false)
    private WriteAfterReadDetector writeAfterReadDetector;
    
    /**
     * 每分钟清理一次过期的写记录
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void cleanupExpiredWrites() {
        if (writeAfterReadDetector != null) {
            try {
                writeAfterReadDetector.cleanupExpiredWrites();
                log.debug("清理过期写记录完成，当前缓存大小: {}", 
                         writeAfterReadDetector.getGlobalWriteCacheSize());
            } catch (Exception e) {
                log.error("清理过期写记录失败", e);
            }
        }
    }
}
