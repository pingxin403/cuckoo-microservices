package com.pingxin403.cuckoo.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Event publisher utility that wraps KafkaEventPublisher.
 * Automatically populates eventId, timestamp, and traceId for all events.
 * 
 * <p>Features:
 * <ul>
 *   <li>Auto-generates eventId if not present (UUID)</li>
 *   <li>Auto-populates timestamp if not present (current time in milliseconds)</li>
 *   <li>Auto-propagates traceId from MDC context</li>
 *   <li>Unified logging for all event publications</li>
 *   <li>Error logging on publication failure</li>
 *   <li>Delegates to KafkaEventPublisher for actual sending</li>
 * </ul>
 * 
 * <p>Usage Example:
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class OrderService {
 *     private final EventPublisherUtil eventPublisher;
 *     
 *     public void createOrder(CreateOrderRequest request) {
 *         Order order = new Order(request);
 *         orderRepository.save(order);
 *         
 *         // Publish event - metadata auto-populated
 *         OrderCreatedEvent event = new OrderCreatedEvent(order.getId(), order.getUserId());
 *         eventPublisher.publish(event);  // eventId, timestamp, traceId auto-set
 *     }
 * }
 * }</pre>
 * 
 * <p>Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 8.2
 * 
 * @author cuckoo-team
 * @see KafkaEventPublisher
 * @see DomainEvent
 */
@Slf4j
@Component
@ConditionalOnClass(name = "org.springframework.kafka.support.SendResult")
@RequiredArgsConstructor
public class EventPublisherUtil {

    private final KafkaEventPublisher kafkaEventPublisher;

    /**
     * Publishes a domain event with auto-populated metadata.
     * 
     * <p>This method automatically:
     * <ul>
     *   <li>Generates a unique eventId (UUID) if not present</li>
     *   <li>Sets the timestamp to current time if not present</li>
     *   <li>Propagates traceId from MDC context if not present</li>
     *   <li>Logs the event publication with eventId, eventType, and traceId</li>
     *   <li>Logs any publication errors with full context</li>
     * </ul>
     * 
     * <p>Example:
     * <pre>{@code
     * OrderCreatedEvent event = new OrderCreatedEvent(orderId, userId);
     * eventPublisher.publish(event);
     * // eventId, timestamp, and traceId are automatically set
     * }</pre>
     * 
     * @param event Domain event to publish
     * @param <T> Event type extending DomainEvent
     * @return CompletableFuture with send result
     */
    public <T extends DomainEvent> CompletableFuture<SendResult<String, DomainEvent>> publish(T event) {
        enrichEvent(event);
        logEventPublication(event);
        
        return kafkaEventPublisher.publish(event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    logEventPublicationError(event, ex);
                }
            });
    }

    /**
     * Publishes a domain event to a specific topic with auto-populated metadata.
     * 
     * <p>This method provides the same auto-population features as {@link #publish(DomainEvent)}
     * but allows specifying a custom Kafka topic.
     * 
     * <p>Example:
     * <pre>{@code
     * OrderCreatedEvent event = new OrderCreatedEvent(orderId, userId);
     * eventPublisher.publish("custom-order-events", event);
     * }</pre>
     * 
     * @param topic Kafka topic to publish to
     * @param event Domain event to publish
     * @param <T> Event type extending DomainEvent
     * @return CompletableFuture with send result
     */
    public <T extends DomainEvent> CompletableFuture<SendResult<String, DomainEvent>> publish(
            String topic, T event) {
        enrichEvent(event);
        logEventPublication(event);
        
        return kafkaEventPublisher.publish(topic, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    logEventPublicationError(event, ex);
                }
            });
    }

    /**
     * Publishes a domain event to a specific topic with a custom key and auto-populated metadata.
     * 
     * <p>This method provides the same auto-population features as {@link #publish(DomainEvent)}
     * but allows specifying both a custom Kafka topic and a custom message key for partitioning.
     * 
     * <p>Example:
     * <pre>{@code
     * OrderCreatedEvent event = new OrderCreatedEvent(orderId, userId);
     * eventPublisher.publish("order-events", orderId.toString(), event);
     * }</pre>
     * 
     * @param topic Kafka topic to publish to
     * @param key Message key for partitioning
     * @param event Domain event to publish
     * @param <T> Event type extending DomainEvent
     * @return CompletableFuture with send result
     */
    public <T extends DomainEvent> CompletableFuture<SendResult<String, DomainEvent>> publish(
            String topic, String key, T event) {
        enrichEvent(event);
        logEventPublication(event);
        
        return kafkaEventPublisher.publish(topic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    logEventPublicationError(event, ex);
                }
            });
    }

    /**
     * Enriches event with auto-generated metadata.
     * 
     * <p>This method:
     * <ul>
     *   <li>Auto-generates eventId (UUID) if null or empty</li>
     *   <li>Auto-populates timestamp (current time in milliseconds) if null</li>
     *   <li>Auto-propagates traceId from MDC context if null or empty</li>
     * </ul>
     * 
     * @param event Event to enrich
     * @param <T> Event type extending DomainEvent
     */
    private <T extends DomainEvent> void enrichEvent(T event) {
        // Auto-generate eventId if not present
        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        
        // Auto-populate timestamp if not present
        if (event.getTimestamp() == null) {
            event.setTimestamp(System.currentTimeMillis());
        }
        
        // Auto-propagate traceId from MDC if not present
        if (event.getTraceId() == null || event.getTraceId().isEmpty()) {
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                event.setTraceId(traceId);
            }
        }
    }

    /**
     * Logs event publication with metadata.
     * 
     * <p>Log format: [traceId] 发布事件: eventType={eventType}, eventId={eventId}
     * 
     * @param event Event being published
     * @param <T> Event type extending DomainEvent
     */
    private <T extends DomainEvent> void logEventPublication(T event) {
        log.info("[{}] 发布事件: eventType={}, eventId={}", 
            event.getTraceId(), event.getEventType(), event.getEventId());
    }

    /**
     * Logs event publication error with full context.
     * 
     * <p>Log format: [traceId] 事件发布失败: eventType={eventType}, eventId={eventId}, error={errorMessage}
     * 
     * @param event Event that failed to publish
     * @param ex Exception that occurred
     * @param <T> Event type extending DomainEvent
     */
    private <T extends DomainEvent> void logEventPublicationError(T event, Throwable ex) {
        log.error("[{}] 事件发布失败: eventType={}, eventId={}, error={}", 
            event.getTraceId(), event.getEventType(), event.getEventId(), ex.getMessage(), ex);
    }
}
