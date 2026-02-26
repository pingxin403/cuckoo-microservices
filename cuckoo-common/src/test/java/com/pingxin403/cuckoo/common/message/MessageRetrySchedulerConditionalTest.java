package com.pingxin403.cuckoo.common.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingxin403.cuckoo.common.event.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 测试 MessageRetryScheduler 的条件加载
 * 验证当 EventPublisher bean 不存在时，MessageRetryScheduler 不会被加载
 * 
 * Validates: Requirement 1.6 - MessageRetryScheduler 应该有 @ConditionalOnBean(EventPublisher.class) 注解
 */
class MessageRetrySchedulerConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessageRetryScheduler.class))
            .withPropertyValues(
                "cuckoo.message.retry.enabled=true"
            );

    /**
     * 测试 MessageRetryScheduler 有 @ConditionalOnBean 注解
     * 这个测试验证类本身的注解
     */
    @Test
    void messageRetrySchedulerShouldHaveConditionalOnBeanAnnotation() {
        // 验证 MessageRetryScheduler 类有 @ConditionalOnBean 注解
        assertThat(MessageRetryScheduler.class)
                .hasAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnBean.class);
        
        // 获取注解并验证其值
        org.springframework.boot.autoconfigure.condition.ConditionalOnBean annotation = 
                MessageRetryScheduler.class.getAnnotation(
                        org.springframework.boot.autoconfigure.condition.ConditionalOnBean.class);
        
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains(EventPublisher.class);
    }

    /**
     * 测试 MessageRetryScheduler 在 EventPublisher bean 可用时被加载
     */
    @Test
    void messageRetrySchedulerShouldLoadWhenEventPublisherBeanExists() {
        contextRunner
                .withBean("eventPublisher", EventPublisher.class, () -> mock(EventPublisher.class))
                .withBean("localMessageService", LocalMessageService.class, () -> mock(LocalMessageService.class))
                .withBean("objectMapper", ObjectMapper.class, ObjectMapper::new)
                .run(context -> {
                    // 当 EventPublisher bean 存在时，MessageRetryScheduler 应该被创建
                    assertThat(context).hasSingleBean(MessageRetryScheduler.class);
                    
                    // 验证它是正确的类型
                    assertThat(context.getBean(MessageRetryScheduler.class))
                            .isInstanceOf(MessageRetryScheduler.class);
                });
    }

    /**
     * 测试 MessageRetryScheduler 在 EventPublisher bean 不存在时不被加载
     */
    @Test
    void messageRetrySchedulerShouldNotLoadWhenEventPublisherBeanDoesNotExist() {
        contextRunner
                .withBean("localMessageService", LocalMessageService.class, () -> mock(LocalMessageService.class))
                .withBean("objectMapper", ObjectMapper.class, ObjectMapper::new)
                .run(context -> {
                    // 当 EventPublisher bean 不存在时，MessageRetryScheduler 不应该被创建
                    assertThat(context).doesNotHaveBean(MessageRetryScheduler.class);
                });
    }

    /**
     * 测试 @ConditionalOnBean 注解的目的
     * 这个测试验证注解的存在性和配置，确保在没有 EventPublisher 的环境中不会加载此 bean
     */
    @Test
    void conditionalOnBeanAnnotationEnsuresEventPublisherDependencyRequired() {
        // 验证 @ConditionalOnBean 注解存在
        org.springframework.boot.autoconfigure.condition.ConditionalOnBean annotation = 
                MessageRetryScheduler.class.getAnnotation(
                        org.springframework.boot.autoconfigure.condition.ConditionalOnBean.class);
        
        assertThat(annotation).isNotNull();
        
        // 验证注解指定了 EventPublisher.class
        assertThat(annotation.value()).hasSize(1);
        assertThat(annotation.value()[0]).isEqualTo(EventPublisher.class);
        
        // 这确保了：
        // 1. 在没有 EventPublisher bean 的项目中，MessageRetryScheduler 不会被加载
        // 2. 在测试环境中，如果没有配置 EventPublisher，此 bean 不会被创建
        // 3. 只有在事件发布器可用时，消息重试调度器才会工作
        // 4. 避免在不需要消息重试功能的服务中加载不必要的调度器
    }

    /**
     * 测试 MessageRetryScheduler 同时需要 @ConditionalOnProperty 和 @ConditionalOnBean
     */
    @Test
    void messageRetrySchedulerShouldRespectBothConditionalAnnotations() {
        // 测试当 property 为 false 时，即使 EventPublisher 存在也不加载
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MessageRetryScheduler.class))
                .withPropertyValues("cuckoo.message.retry.enabled=false")
                .withBean("eventPublisher", EventPublisher.class, () -> mock(EventPublisher.class))
                .withBean("localMessageService", LocalMessageService.class, () -> mock(LocalMessageService.class))
                .withBean("objectMapper", ObjectMapper.class, ObjectMapper::new)
                .run(context -> {
                    // 当 property 为 false 时，不应该加载
                    assertThat(context).doesNotHaveBean(MessageRetryScheduler.class);
                });
    }
}
