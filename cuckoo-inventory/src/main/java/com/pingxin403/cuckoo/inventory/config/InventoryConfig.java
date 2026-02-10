package com.pingxin403.cuckoo.inventory.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 库存服务配置
 * 支持 Nacos Config 动态刷新（@RefreshScope）
 */
@Data
@Component
@RefreshScope
public class InventoryConfig {

    /**
     * 分布式锁超时时间（秒），默认 5 秒
     * 可通过 Nacos Config 动态修改
     */
    @Value("${inventory.lock.timeout:5}")
    private int lockTimeout;
}
