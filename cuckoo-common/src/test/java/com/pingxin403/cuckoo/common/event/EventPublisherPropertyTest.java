package com.pingxin403.cuckoo.common.event;

import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * EventPublisher 属性测试
 * 验证事件发布器的正确性属性
 */
class EventPublisherPropertyTest {

    /**
     * 创建 EventPublisher 实例用于测试
     */
    private EventPublisher createEventPublisher() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, DomainEvent> kafkaTemplate = mock(KafkaTemplate.class);
        return new EventPublisher(kafkaTemplate);
    }

    /**
     * 属性 3：EventPublisher 自动填充事件元数据
     * 
     * 对于任意的领域事件，如果事件的 eventId 或 timestamp 为空，
     * 调用 EventPublisher.publish() 后，事件应该被自动填充有效的 
     * eventId（UUID 格式）和 timestamp（当前时间）。
     * 
     * 验证需求：2.3, 2.4
     */
    @Property(tries = 100)
    @Label("Feature: microservice-optimization, Property 3: EventPublisher 自动填充事件元数据")
    void eventPublisher_autoFillsEventMetadata(
            @ForAll("topics") String topic,
            @ForAll("keys") String key,
            @ForAll("testEvents") TestDomainEvent event) {
        
        EventPublisher eventPublisher = createEventPublisher();
        
        // 清空 eventId 和 timestamp 模拟未设置的情况
        event.setEventId(null);
        event.setTimestamp(null);
        
        Instant beforePublish = Instant.now();
        
        // 发布事件
        eventPublisher.publish(topic, key, event);
        
        Instant afterPublish = Instant.now();
        
        // 验证 eventId 已被自动填充且格式正确（UUID 格式）
        assertThat(event.getEventId())
            .isNotNull()
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        
        // 验证 timestamp 已被自动填充且在合理的时间范围内
        assertThat(event.getTimestamp())
            .isNotNull()
            .isAfterOrEqualTo(beforePublish.minusSeconds(1))
            .isBeforeOrEqualTo(afterPublish.plusSeconds(1));
    }

    /**
     * 属性 3（扩展）：EventPublisher 不覆盖已设置的元数据
     * 
     * 如果事件的 eventId 和 timestamp 已经设置，
     * EventPublisher 不应该覆盖它们。
     */
    @Property(tries = 100)
    @Label("Feature: microservice-optimization, Property 3: EventPublisher 不覆盖已设置的元数据")
    void eventPublisher_doesNotOverrideExistingMetadata(
            @ForAll("topics") String topic,
            @ForAll("keys") String key,
            @ForAll("testEvents") TestDomainEvent event) {
        
        EventPublisher eventPublisher = createEventPublisher();
        
        // 设置自定义的 eventId 和 timestamp
        String customEventId = UUID.randomUUID().toString();
        Instant customTimestamp = Instant.now().minusSeconds(3600);
        event.setEventId(customEventId);
        event.setTimestamp(customTimestamp);
        
        // 发布事件
        eventPublisher.publish(topic, key, event);
        
        // 验证 eventId 和 timestamp 没有被覆盖
        assertThat(event.getEventId()).isEqualTo(customEventId);
        assertThat(event.getTimestamp()).isEqualTo(customTimestamp);
    }

    /**
     * 属性 3（扩展）：使用默认 key 的 publish 方法也自动填充元数据
     * 
     * 当使用 publish(topic, event) 方法时（不指定 key），
     * EventPublisher 应该自动填充 eventId 并使用它作为 key。
     */
    @Property(tries = 100)
    @Label("Feature: microservice-optimization, Property 3: 使用默认 key 的 publish 方法也自动填充元数据")
    void eventPublisher_autoFillsMetadataWithDefaultKey(
            @ForAll("topics") String topic,
            @ForAll("testEvents") TestDomainEvent event) {
        
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, DomainEvent> kafkaTemplate = mock(KafkaTemplate.class);
        EventPublisher eventPublisher = new EventPublisher(kafkaTemplate);
        
        // 清空 eventId 和 timestamp
        event.setEventId(null);
        event.setTimestamp(null);
        
        // 使用不指定 key 的 publish 方法
        eventPublisher.publish(topic, event);
        
        // 验证 eventId 和 timestamp 已被自动填充
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getTimestamp()).isNotNull();
        
        // 验证 Kafka 发送时使用 eventId 作为 key
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(topic), keyCaptor.capture(), eq(event));
        assertThat(keyCaptor.getValue()).isEqualTo(event.getEventId());
    }

    // ========== 数据生成器 ==========

    @Provide
    Arbitrary<String> topics() {
        return Arbitraries.of(
            "order-events",
            "payment-events",
            "user-events",
            "product-events",
            "inventory-events"
        );
    }

    @Provide
    Arbitrary<String> keys() {
        return Arbitraries.strings()
            .numeric()
            .ofMinLength(1)
            .ofMaxLength(10);
    }

    @Provide
    Arbitrary<TestDomainEvent> testEvents() {
        return Arbitraries.strings()
            .alpha()
            .ofMinLength(5)
            .ofMaxLength(20)
            .map(TestDomainEvent::new);
    }

    /**
     * 属性 4：EventPublisher 记录完整的事件发布日志
     * 
     * 对于任意的领域事件，调用 EventPublisher.publish() 成功后，
     * 日志系统应该记录包含 topic、key、eventType、eventId 的日志条目。
     * 
     * 验证需求：2.5
     */
    @Property(tries = 100)
    @Label("Feature: microservice-optimization, Property 4: EventPublisher 记录完整的事件发布日志")
    void eventPublisher_logsCompleteEventInformation(
            @ForAll("topics") String topic,
            @ForAll("keys") String key,
            @ForAll("testEvents") TestDomainEvent event) {
        
        // 创建一个 ListAppender 来捕获日志
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) 
            org.slf4j.LoggerFactory.getLogger(EventPublisher.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> listAppender = 
            new ch.qos.logback.core.read.ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        try {
            EventPublisher eventPublisher = createEventPublisher();
            
            // 确保事件有 eventId（用于日志验证）
            if (event.getEventId() == null || event.getEventId().isEmpty()) {
                event.setEventId(UUID.randomUUID().toString());
            }
            
            // 发布事件
            eventPublisher.publish(topic, key, event);
            
            // 验证日志记录
            java.util.List<ch.qos.logback.classic.spi.ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).isNotEmpty();
            
            // 查找包含 "发布事件成功" 的日志
            boolean foundLog = logsList.stream()
                .anyMatch(logEvent -> {
                    String message = logEvent.getFormattedMessage();
                    return message.contains("发布事件成功") &&
                           message.contains("topic=" + topic) &&
                           message.contains("key=" + key) &&
                           message.contains("eventType=" + event.getEventType()) &&
                           message.contains("eventId=" + event.getEventId());
                });
            
            assertThat(foundLog)
                .as("日志应该包含 topic、key、eventType、eventId")
                .isTrue();
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    /**
     * 属性 4（扩展）：EventPublisher 在发布失败时记录错误日志
     * 
     * 当 Kafka 发送失败时，EventPublisher 应该记录错误日志，
     * 但不应该抛出异常（避免阻塞业务流程）。
     */
    @Property(tries = 100)
    @Label("Feature: microservice-optimization, Property 4: EventPublisher 在发布失败时记录错误日志")
    void eventPublisher_logsErrorOnPublishFailure(
            @ForAll("topics") String topic,
            @ForAll("keys") String key,
            @ForAll("testEvents") TestDomainEvent event) {
        
        // 创建一个会抛出异常的 KafkaTemplate
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, DomainEvent> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(any(), any(), any()))
            .thenThrow(new RuntimeException("Kafka connection failed"));
        
        EventPublisher eventPublisher = new EventPublisher(kafkaTemplate);
        
        // 创建一个 ListAppender 来捕获日志
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) 
            org.slf4j.LoggerFactory.getLogger(EventPublisher.class);
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> listAppender = 
            new ch.qos.logback.core.read.ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        
        try {
            // 确保事件有 eventId
            if (event.getEventId() == null || event.getEventId().isEmpty()) {
                event.setEventId(UUID.randomUUID().toString());
            }
            
            // 发布事件（应该不抛出异常）
            eventPublisher.publish(topic, key, event);
            
            // 验证错误日志记录
            java.util.List<ch.qos.logback.classic.spi.ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).isNotEmpty();
            
            // 查找包含 "发布事件失败" 的错误日志
            boolean foundErrorLog = logsList.stream()
                .anyMatch(logEvent -> {
                    String message = logEvent.getFormattedMessage();
                    return logEvent.getLevel() == ch.qos.logback.classic.Level.ERROR &&
                           message.contains("发布事件失败") &&
                           message.contains("topic=" + topic) &&
                           message.contains("key=" + key) &&
                           message.contains("eventType=" + event.getEventType()) &&
                           message.contains("eventId=" + event.getEventId());
                });
            
            assertThat(foundErrorLog)
                .as("错误日志应该包含 topic、key、eventType、eventId")
                .isTrue();
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    /**
     * 测试用的领域事件实现
     */
    static class TestDomainEvent extends DomainEvent {
        private String data;

        public TestDomainEvent(String data) {
            this.data = data;
            setEventType("TEST_EVENT");
            setVersion("1.0");
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
