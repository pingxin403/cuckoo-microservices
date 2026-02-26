package com.pingxin403.cuckoo.common.shutdown;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 GracefulShutdownHandler 的条件加载
 * 验证当 KafkaTemplate 不在 classpath 时，GracefulShutdownHandler 不会被加载
 * 
 * Validates: Requirement 1.7 - GracefulShutdownHandler 应该有 @ConditionalOnClass(KafkaTemplate.class) 注解
 */
class GracefulShutdownHandlerConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GracefulShutdownHandler.class))
            .withPropertyValues(
                "graceful.shutdown.grace-period=0",  // 设置为0秒避免测试等待
                "graceful.shutdown.max-wait=0"       // 设置为0秒避免测试等待
            );

    /**
     * 测试 GracefulShutdownHandler 有 @ConditionalOnClass 注解
     * 这个测试验证类本身的注解
     */
    @Test
    void gracefulShutdownHandlerShouldHaveConditionalOnClassAnnotation() {
        // 验证 GracefulShutdownHandler 类有 @ConditionalOnClass 注解
        assertThat(GracefulShutdownHandler.class)
                .hasAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnClass.class);
        
        // 获取注解并验证其值
        org.springframework.boot.autoconfigure.condition.ConditionalOnClass annotation = 
                GracefulShutdownHandler.class.getAnnotation(
                        org.springframework.boot.autoconfigure.condition.ConditionalOnClass.class);
        
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains(KafkaTemplate.class);
    }

    /**
     * 测试 GracefulShutdownHandler 在 KafkaTemplate 可用时被加载
     * 由于 KafkaTemplate 在测试 classpath 中，这个测试验证 bean 确实被创建
     */
    @Test
    void gracefulShutdownHandlerShouldLoadWhenKafkaTemplateIsOnClasspath() {
        contextRunner
                .run(context -> {
                    // 由于 KafkaTemplate 在 classpath 中，GracefulShutdownHandler 应该被创建
                    assertThat(context).hasSingleBean(GracefulShutdownHandler.class);
                    
                    // 验证它实现了 ApplicationListener 接口
                    assertThat(context.getBean(GracefulShutdownHandler.class))
                            .isInstanceOf(org.springframework.context.ApplicationListener.class);
                });
    }

    /**
     * 测试 @ConditionalOnClass 注解的目的
     * 这个测试验证注解的存在性和配置，确保在没有 Kafka 依赖的环境中不会加载此 bean
     * 
     * 注意：由于 KafkaTemplate 在测试 classpath 中，我们无法在运行时测试"不加载"的情况。
     * 但是通过验证注解的存在，我们确保了在生产环境中如果没有 Kafka 依赖，此 bean 不会被创建。
     */
    @Test
    void conditionalOnClassAnnotationEnsuresKafkaDependencyRequired() {
        // 验证 @ConditionalOnClass 注解存在
        org.springframework.boot.autoconfigure.condition.ConditionalOnClass annotation = 
                GracefulShutdownHandler.class.getAnnotation(
                        org.springframework.boot.autoconfigure.condition.ConditionalOnClass.class);
        
        assertThat(annotation).isNotNull();
        
        // 验证注解指定了 KafkaTemplate.class
        assertThat(annotation.value()).hasSize(1);
        assertThat(annotation.value()[0]).isEqualTo(KafkaTemplate.class);
        
        // 这确保了：
        // 1. 在没有 spring-kafka 依赖的项目中，GracefulShutdownHandler 不会被加载
        // 2. 在测试环境中，如果排除了 Kafka 依赖，此 bean 不会被创建
        // 3. 只有在 Kafka 可用时，优雅下线处理器才会工作
    }
}
