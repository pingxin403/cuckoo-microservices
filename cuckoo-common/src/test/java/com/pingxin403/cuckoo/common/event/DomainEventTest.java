package com.pingxin403.cuckoo.common.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 领域事件基类和具体事件类的单元测试
 */
class DomainEventTest {

    @Test
    void orderCreatedEvent_shouldHaveCorrectFields() {
        long before = System.currentTimeMillis();
        OrderCreatedEvent event = OrderCreatedEvent.create(1L, 100L, 200L, 5, new BigDecimal("99.99"));
        long after = System.currentTimeMillis();

        assertNotNull(event.getEventId());
        assertFalse(event.getEventId().isEmpty());
        assertEquals("ORDER_CREATED", event.getEventType());
        assertEquals(1, event.getVersion());
        assertNotNull(event.getTimestamp());
        assertTrue(event.getTimestamp() >= before && event.getTimestamp() <= after);
        assertEquals(1L, event.getOrderId());
        assertEquals(100L, event.getUserId());
        assertEquals(200L, event.getSkuId());
        assertEquals(5, event.getQuantity());
        assertEquals(new BigDecimal("99.99"), event.getTotalAmount());
        assertNotNull(event.getPayload());
    }

    @Test
    void orderCancelledEvent_shouldHaveCorrectFields() {
        OrderCancelledEvent event = OrderCancelledEvent.create(1L, 100L, 200L, 3, "用户取消");

        assertNotNull(event.getEventId());
        assertEquals("ORDER_CANCELLED", event.getEventType());
        assertEquals(1, event.getVersion());
        assertNotNull(event.getTimestamp());
        assertEquals(1L, event.getOrderId());
        assertEquals(100L, event.getUserId());
        assertEquals(200L, event.getSkuId());
        assertEquals(3, event.getQuantity());
        assertEquals("用户取消", event.getReason());
        assertNotNull(event.getPayload());
    }

    @Test
    void paymentSuccessEvent_shouldHaveCorrectFields() {
        PaymentSuccessEvent event = PaymentSuccessEvent.create(1L, 10L, 100L, new BigDecimal("199.00"), "ALIPAY");

        assertNotNull(event.getEventId());
        assertEquals("PAYMENT_SUCCESS", event.getEventType());
        assertEquals(1, event.getVersion());
        assertNotNull(event.getTimestamp());
        assertEquals(1L, event.getOrderId());
        assertEquals(10L, event.getPaymentId());
        assertEquals(100L, event.getUserId());
        assertEquals(new BigDecimal("199.00"), event.getAmount());
        assertEquals("ALIPAY", event.getPaymentMethod());
        assertNotNull(event.getPayload());
    }

    @Test
    void paymentFailedEvent_shouldHaveCorrectFields() {
        PaymentFailedEvent event = PaymentFailedEvent.create(1L, 10L, 100L, "余额不足");

        assertNotNull(event.getEventId());
        assertEquals("PAYMENT_FAILED", event.getEventType());
        assertEquals(1, event.getVersion());
        assertNotNull(event.getTimestamp());
        assertEquals(1L, event.getOrderId());
        assertEquals(10L, event.getPaymentId());
        assertEquals(100L, event.getUserId());
        assertEquals("余额不足", event.getReason());
        assertNotNull(event.getPayload());
    }

    @Test
    void differentEvents_shouldHaveDifferentEventIds() {
        OrderCreatedEvent event1 = OrderCreatedEvent.create(1L, 100L, 200L, 1, BigDecimal.TEN);
        OrderCreatedEvent event2 = OrderCreatedEvent.create(2L, 100L, 200L, 1, BigDecimal.TEN);

        assertNotEquals(event1.getEventId(), event2.getEventId());
    }

    @Test
    void event_noArgConstructor_shouldCreateEmptyEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent();
        assertNull(event.getEventId());
        assertNull(event.getEventType());
        assertNull(event.getTimestamp());
        assertNull(event.getVersion());
    }

    @Test
    void event_shouldSupportPayloadOperations() {
        OrderCreatedEvent event = OrderCreatedEvent.create(1L, 100L, 200L, 5, new BigDecimal("99.99"));
        
        // Test adding payload
        event.addPayload("source", "mobile");
        event.addPayload("channel", "app");
        
        // Test getting payload
        assertEquals("mobile", event.getPayload("source"));
        assertEquals("app", event.getPayload("channel"));
        assertNull(event.getPayload("nonexistent"));
    }
}
