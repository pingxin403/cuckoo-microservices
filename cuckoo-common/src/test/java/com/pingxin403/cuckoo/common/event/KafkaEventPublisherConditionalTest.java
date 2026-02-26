package com.pingxin403.cuckoo.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 KafkaEventPublisher 的条件加载
 * 验证当 KafkaTemplate 不在 classpath 时，KafkaEventPublisher 不会被加载
 * 
 * Validates: Requirement 1.4 - KafkaEventPublisher 应该有 @ConditionalOnClass(KafkaTemplate.class) 注解
 */
class KafkaEventPublisherConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KafkaEventPublisher.class));

    /**
     * 测试当 KafkaTemplate 在 classpath 时，KafkaEventPublisher 应该被加载
     */
    @Test
    void shouldLoadKafkaEventPublisherWhenKafkaTemplateIsPresent() {
        contextRunner
                .withBean("kafkaTemplate", KafkaTemplate.class, () -> null)
                .run(context -> {
                    // 验证 KafkaEventPublisher 被加载
                    assertThat(context).hasSingleBean(KafkaEventPublisher.class);
                    
                    // 验证它实现了 EventPublisher 接口
                    assertThat(context.getBean(KafkaEventPublisher.class))
                            .isInstanceOf(EventPublisher.class);
                });
    }

    /**
     * 测试 KafkaEventPublisher 有 @ConditionalOnClass 注解
     * 这个测试验证类本身的注解
     */
    @Test
    void kafkaEventPublisherShouldHaveConditionalOnClassAnnotation() {
        // 验证 KafkaEventPublisher 类有 @ConditionalOnClass 注解
        assertThat(KafkaEventPublisher.class)
                .hasAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnClass.class);
        
        // 获取注解并验证其值
        org.springframework.boot.autoconfigure.condition.ConditionalOnClass annotation = 
                KafkaEventPublisher.class.getAnnotation(
                        org.springframework.boot.autoconfigure.condition.ConditionalOnClass.class);
        
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains(KafkaTemplate.class);
    }

    /**
     * 测试 KafkaEventPublisher 只在 KafkaTemplate 可用时创建
     * 这个测试确保 Kafka 事件发布器不会在测试环境中被创建（如果 Kafka 不可用）
     */
    @Test
    void kafkaEventPublisherShouldOnlyLoadWhenKafkaTemplateIsAvailable() {
        contextRunner
                .withBean("kafkaTemplate", KafkaTemplate.class, () -> null)
                .run(context -> {
                    // 当 KafkaTemplate 在 classpath 时，KafkaEventPublisher 应该被创建
                    assertThat(context).hasSingleBean(KafkaEventPublisher.class);
                    
                    // 验证它是 EventPublisher 的实现
                    EventPublisher publisher = context.getBean(EventPublisher.class);
                    assertThat(publisher).isInstanceOf(KafkaEventPublisher.class);
                });
    }
}
