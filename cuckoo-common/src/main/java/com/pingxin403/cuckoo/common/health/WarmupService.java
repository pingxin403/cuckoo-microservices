package com.pingxin403.cuckoo.common.health;

/**
 * 服务预热接口
 * 各个服务可以实现此接口来定义自己的预热逻辑
 */
public interface WarmupService {
    
    /**
     * 执行预热逻辑
     * 包括：
     * - 初始化数据库连接池
     * - 初始化 Redis 连接池
     * - 加载热点数据到缓存
     * - 触发 JVM 类加载和 JIT 编译
     * 
     * @throws Exception 如果预热失败
     */
    void performWarmup() throws Exception;
}
