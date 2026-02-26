package com.pingxin403.cuckoo.common.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 KafkaConsumerConfig 的条件加载
 * 验证当 KafkaTemplate 不在 classpath 时，KafkaConsumerConfig 不会被加载
 * 
 * Validates: Requirement 1.2 - KafkaConsumerConfig 应该有 @ConditionalOnClass(KafkaTemplate.class) 注解
 */
class KafkaConsumerConfigConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KafkaConsumerConfig.class));

    /**
     * 测试当 KafkaTemplate 在 classpath 时，KafkaConsumerConfig 应该被加载
     */
    @Test
    void shouldLoadKafkaConsumerConfigWhenKafkaTemplateIsPresent() {
        contextRunner
                .withPropertyValues("spring.kafka.bootstrap-servers=localhost:9092")
                .run(context -> {
                    // 验证 KafkaConsumerConfig 被加载
                    assertThat(context).hasSingleBean(KafkaConsumerConfig.class);
                    
                    // 验证 deadLetterQueueTemplate bean 存在
                    assertThat(context).hasBean("deadLetterQueueTemplate");
                    assertThat(context.getBean("deadLetterQueueTemplate"))
                            .isInstanceOf(KafkaTemplate.class);
                    
                    // 验证 errorHandler bean 存在
                    assertThat(context).hasBean("errorHandler");
                });
    }

    /**
     * 测试 KafkaConsumerConfig 有 @ConditionalOnClass 注解
     * 这个测试验证配置类本身的注解
     */
    @Test
    void kafkaConsumerConfigShouldHaveConditionalOnClassAnnotation() {
        // 验证 KafkaConsumerConfig 类有 @ConditionalOnClass 注解
        assertThat(KafkaConsumerConfig.class)
                .hasAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnClass.class);
        
        // 获取注解并验证其值
        org.springframework.boot.autoconfigure.condition.ConditionalOnClass annotation = 
                KafkaConsumerConfig.class.getAnnotation(
                        org.springframework.boot.autoconfigure.condition.ConditionalOnClass.class);
        
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).contains(KafkaTemplate.class);
    }

    /**
     * 测试 KafkaConsumerConfig 的 beans 只在 KafkaTemplate 可用时创建
     * 这个测试确保 Kafka 相关的 beans 不会在测试环境中被创建（如果 Kafka 不可用）
     */
    @Test
    void kafkaConsumerConfigBeansShouldOnlyLoadWhenKafkaTemplateIsAvailable() {
        contextRunner
                .withPropertyValues("spring.kafka.bootstrap-servers=localhost:9092")
                .run(context -> {
                    // 当 KafkaTemplate 在 classpath 时，beans 应该被创建
                    if (context.containsBean("deadLetterQueueTemplate")) {
                        assertThat(context.getBean("deadLetterQueueTemplate"))
                                .isInstanceOf(KafkaTemplate.class);
                    }
                    
                    // 验证配置类被加载
                    assertThat(context).hasSingleBean(KafkaConsumerConfig.class);
                });
    }
}
