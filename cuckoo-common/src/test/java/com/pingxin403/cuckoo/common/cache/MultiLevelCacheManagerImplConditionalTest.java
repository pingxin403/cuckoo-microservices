package com.pingxin403.cuckoo.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 MultiLevelCacheManagerImpl 的条件加载
 * 验证当 Caffeine 或 RedisTemplate 不在 classpath 时，MultiLevelCacheManagerImpl 不会被加载
 * 
 * Validates: Requirement 1.5 - MultiLevelCacheManagerImpl 应该有 @ConditionalOnClass 注解
 */
class MultiLevelCacheManagerImplConditionalTest {

    /**
     * 测试 MultiLevelCacheManagerImpl 有 @ConditionalOnClass 注解
     * 这个测试验证类本身的注解
     */
    @Test
    void multiLevelCacheManagerImplShouldHaveConditionalOnClassAnnotation() {
        // 验证 MultiLevelCacheManagerImpl 类有 @ConditionalOnClass 注解
        assertThat(MultiLevelCacheManagerImpl.class)
                .hasAnnotation(ConditionalOnClass.class);
        
        // 获取注解并验证其值
        ConditionalOnClass annotation = 
                MultiLevelCacheManagerImpl.class.getAnnotation(ConditionalOnClass.class);
        
        assertThat(annotation).isNotNull();
        
        // 验证注解同时指定了 Caffeine.class 和 RedisTemplate
        assertThat(annotation.value()).contains(Caffeine.class);
        assertThat(annotation.name()).contains("org.springframework.data.redis.core.RedisTemplate");
    }

    /**
     * 测试 @ConditionalOnClass 注解的目的
     * 这个测试验证注解的存在性和配置，确保在没有 Caffeine 或 Redis 依赖的环境中不会加载此 bean
     * 
     * 注意：由于 Caffeine 和 RedisTemplate 在测试 classpath 中，我们无法在运行时测试"不加载"的情况。
     * 但是通过验证注解的存在，我们确保了在生产环境中如果没有这些依赖，此 bean 不会被创建。
     */
    @Test
    void conditionalOnClassAnnotationEnsuresDependenciesRequired() {
        // 获取 @ConditionalOnClass 注解
        ConditionalOnClass annotation = 
                MultiLevelCacheManagerImpl.class.getAnnotation(ConditionalOnClass.class);
        
        assertThat(annotation).isNotNull();
        
        // 验证注解同时指定了 Caffeine.class (value) 和 RedisTemplate (name)
        assertThat(annotation.value()).contains(Caffeine.class);
        assertThat(annotation.name()).contains("org.springframework.data.redis.core.RedisTemplate");
        
        // 这确保了：
        // 1. 在没有 Caffeine 依赖的项目中，MultiLevelCacheManagerImpl 不会被加载
        // 2. 在没有 Redis 依赖的项目中，MultiLevelCacheManagerImpl 不会被加载
        // 3. 在测试环境中，如果排除了这些依赖，此 bean 不会被创建
        // 4. 只有在 Caffeine 和 Redis 都可用时，多级缓存管理器才会工作
        // 5. 避免在测试环境中因缺少依赖而导致 ApplicationContext 加载失败
    }

    /**
     * 测试 MultiLevelCacheManagerImpl 实现了 MultiLevelCacheManager 接口
     */
    @Test
    void multiLevelCacheManagerImplShouldImplementInterface() {
        // 验证 MultiLevelCacheManagerImpl 实现了 MultiLevelCacheManager 接口
        assertThat(MultiLevelCacheManager.class)
                .isAssignableFrom(MultiLevelCacheManagerImpl.class);
    }
}
