package com.pingxin403.cuckoo.order.service;

import com.pingxin403.cuckoo.order.TestOrderApplication;
import com.pingxin403.cuckoo.order.config.TestConfig;
import com.pingxin403.cuckoo.order.dto.CreateOrderRequest;
import com.pingxin403.cuckoo.order.dto.OrderDTO;
import com.pingxin403.cuckoo.order.repository.OrderRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Positive;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for OrderService
 *
 * Uses TestOrderApplication (no Feign/Kafka) with mock beans from TestConfig.
 */
@JqwikSpringSupport
@SpringBootTest(classes = TestOrderApplication.class)
@ActiveProfiles("test")
@Import(TestConfig.class)
class OrderServicePropertyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Property: Order total amount equals unit price * quantity
     * Validates: Requirements 5.1
     */
    @Property(tries = 100)
    @Label("Order total amount equals unit price times quantity")
    @Transactional
    void orderCreation_calculatesAmountCorrectly(
            @ForAll @Positive Long userId,
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 1, max = 100) int quantity) {
        orderRepository.deleteAll();
        CreateOrderRequest request = new CreateOrderRequest(userId, skuId, quantity);
        OrderDTO order = orderService.createOrder(request);

        BigDecimal expected = order.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
        assertThat(order.getTotalAmount()).isEqualByComparingTo(expected);
        assertThat(order.getQuantity()).isEqualTo(quantity);
        assertThat(order.getUserId()).isEqualTo(userId);
        assertThat(order.getSkuId()).isEqualTo(skuId);
    }

    /**
     * Property: Newly created orders have PENDING_PAYMENT status
     * Validates: Requirements 5.1
     */
    @Property(tries = 100)
    @Label("Newly created orders start with PENDING_PAYMENT status")
    @Transactional
    void orderCreation_setsInitialStatusToPendingPayment(
            @ForAll @Positive Long userId,
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 1, max = 100) int quantity) {
        orderRepository.deleteAll();
        CreateOrderRequest request = new CreateOrderRequest(userId, skuId, quantity);
        OrderDTO order = orderService.createOrder(request);

        assertThat(order.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(order.getOrderNo()).isNotNull().startsWith("ORD");
    }

    /**
     * Property: Order cancellation changes status to CANCELLED
     * Validates: Requirements 5.2
     */
    @Property(tries = 100)
    @Label("Order cancellation changes status from PENDING_PAYMENT to CANCELLED")
    @Transactional
    void orderCancellation_changesStatusToCancelled(
            @ForAll @Positive Long userId,
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 1, max = 100) int quantity) {
        orderRepository.deleteAll();
        CreateOrderRequest request = new CreateOrderRequest(userId, skuId, quantity);
        OrderDTO created = orderService.createOrder(request);

        OrderDTO cancelled = orderService.cancelOrder(created.getId());

        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(cancelled.getCancelReason()).isNotNull();
    }

    /**
     * Property: Only PENDING_PAYMENT orders can be cancelled
     * Validates: Requirements 5.2
     */
    @Property(tries = 100)
    @Label("Only PENDING_PAYMENT orders can be cancelled")
    @Transactional
    void orderCancellation_onlyAllowsPendingPaymentOrders(
            @ForAll @Positive Long userId,
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 1, max = 100) int quantity) {
        orderRepository.deleteAll();
        CreateOrderRequest request = new CreateOrderRequest(userId, skuId, quantity);
        OrderDTO created = orderService.createOrder(request);
        orderService.cancelOrder(created.getId());

        assertThatThrownBy(() -> orderService.cancelOrder(created.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只能取消待支付状态的订单");
    }

    /**
     * Property: Payment update changes status to PAID
     * Validates: Requirements 5.3
     */
    @Property(tries = 100)
    @Label("Order payment update changes status from PENDING_PAYMENT to PAID")
    @Transactional
    void orderPayment_changesStatusToPaid(
            @ForAll @Positive Long userId,
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 1, max = 100) int quantity,
            @ForAll @Positive Long paymentId) {
        orderRepository.deleteAll();
        CreateOrderRequest request = new CreateOrderRequest(userId, skuId, quantity);
        OrderDTO created = orderService.createOrder(request);

        orderService.updateOrderToPaid(created.getId(), paymentId);

        OrderDTO paid = orderService.getOrder(created.getId());
        assertThat(paid.getStatus()).isEqualTo("PAID");
        assertThat(paid.getPaymentId()).isEqualTo(paymentId);
    }

    /**
     * Property: Order query returns correct information
     * Validates: Requirements 5.4
     */
    @Property(tries = 100)
    @Label("Order query returns correct order information")
    @Transactional
    void orderQuery_returnsCorrectInformation(
            @ForAll @Positive Long userId,
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 1, max = 100) int quantity) {
        orderRepository.deleteAll();
        CreateOrderRequest request = new CreateOrderRequest(userId, skuId, quantity);
        OrderDTO created = orderService.createOrder(request);

        OrderDTO queried = orderService.getOrder(created.getId());

        assertThat(queried.getId()).isEqualTo(created.getId());
        assertThat(queried.getOrderNo()).isEqualTo(created.getOrderNo());
        assertThat(queried.getUserId()).isEqualTo(created.getUserId());
        assertThat(queried.getSkuId()).isEqualTo(created.getSkuId());
        assertThat(queried.getQuantity()).isEqualTo(created.getQuantity());
        assertThat(queried.getTotalAmount()).isEqualByComparingTo(created.getTotalAmount());
    }

    /**
     * Property: User orders query returns all orders for that user
     * Validates: Requirements 5.4
     */
    @Property(tries = 100)
    @Label("User orders query returns all orders for that user")
    @Transactional
    void userOrdersQuery_returnsAllUserOrders(
            @ForAll @Positive Long userId,
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 1, max = 5) int orderCount) {
        orderRepository.deleteAll();
        for (int i = 0; i < orderCount; i++) {
            orderService.createOrder(new CreateOrderRequest(userId, skuId, i + 1));
        }

        var userOrders = orderService.getUserOrders(userId);

        assertThat(userOrders).hasSize(orderCount);
        assertThat(userOrders).allMatch(o -> o.getUserId().equals(userId));
    }
}
