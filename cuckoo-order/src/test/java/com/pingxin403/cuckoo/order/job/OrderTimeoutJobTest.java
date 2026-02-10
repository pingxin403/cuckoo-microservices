package com.pingxin403.cuckoo.order.job;

import com.pingxin403.cuckoo.order.entity.Order;
import com.pingxin403.cuckoo.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 订单超时取消定时任务测试
 */
@ExtendWith(MockitoExtension.class)
class OrderTimeoutJobTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderTimeoutJob orderTimeoutJob;

    private Order timeoutOrder1;
    private Order timeoutOrder2;

    @BeforeEach
    void setUp() {
        // 设置超时时间为 30 分钟
        ReflectionTestUtils.setField(orderTimeoutJob, "timeoutMinutes", 30);

        // 创建测试订单
        timeoutOrder1 = new Order();
        timeoutOrder1.setId(1L);
        timeoutOrder1.setOrderNo("ORD123456");
        timeoutOrder1.setUserId(1L);
        timeoutOrder1.setSkuId(100L);
        timeoutOrder1.setProductName("Test Product 1");
        timeoutOrder1.setQuantity(2);
        timeoutOrder1.setUnitPrice(new BigDecimal("50.00"));
        timeoutOrder1.setTotalAmount(new BigDecimal("100.00"));
        timeoutOrder1.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        timeoutOrder1.setCreatedAt(LocalDateTime.now().minusMinutes(35));

        timeoutOrder2 = new Order();
        timeoutOrder2.setId(2L);
        timeoutOrder2.setOrderNo("ORD789012");
        timeoutOrder2.setUserId(2L);
        timeoutOrder2.setSkuId(200L);
        timeoutOrder2.setProductName("Test Product 2");
        timeoutOrder2.setQuantity(1);
        timeoutOrder2.setUnitPrice(new BigDecimal("80.00"));
        timeoutOrder2.setTotalAmount(new BigDecimal("80.00"));
        timeoutOrder2.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        timeoutOrder2.setCreatedAt(LocalDateTime.now().minusMinutes(40));
    }

    @Test
    void cancelTimeoutOrders_shouldCancelAllTimeoutOrders() {
        // Given: 有两个超时订单
        List<Order> timeoutOrders = Arrays.asList(timeoutOrder1, timeoutOrder2);
        when(orderService.findTimeoutOrders(any(LocalDateTime.class))).thenReturn(timeoutOrders);
        doNothing().when(orderService).cancelTimeoutOrder(any(Order.class));

        // When: 执行定时任务
        orderTimeoutJob.cancelTimeoutOrders();

        // Then: 应该查询超时订单并取消所有订单
        verify(orderService).findTimeoutOrders(any(LocalDateTime.class));
        verify(orderService).cancelTimeoutOrder(timeoutOrder1);
        verify(orderService).cancelTimeoutOrder(timeoutOrder2);
    }

    @Test
    void cancelTimeoutOrders_shouldHandleNoTimeoutOrders() {
        // Given: 没有超时订单
        when(orderService.findTimeoutOrders(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        // When: 执行定时任务
        orderTimeoutJob.cancelTimeoutOrders();

        // Then: 应该查询超时订单但不执行取消操作
        verify(orderService).findTimeoutOrders(any(LocalDateTime.class));
        verify(orderService, never()).cancelTimeoutOrder(any(Order.class));
    }

    @Test
    void cancelTimeoutOrders_shouldContinueWhenOneOrderFails() {
        // Given: 有两个超时订单，第一个取消失败
        List<Order> timeoutOrders = Arrays.asList(timeoutOrder1, timeoutOrder2);
        when(orderService.findTimeoutOrders(any(LocalDateTime.class))).thenReturn(timeoutOrders);
        doThrow(new RuntimeException("Database error")).when(orderService).cancelTimeoutOrder(timeoutOrder1);
        doNothing().when(orderService).cancelTimeoutOrder(timeoutOrder2);

        // When: 执行定时任务
        orderTimeoutJob.cancelTimeoutOrders();

        // Then: 应该继续处理第二个订单
        verify(orderService).findTimeoutOrders(any(LocalDateTime.class));
        verify(orderService).cancelTimeoutOrder(timeoutOrder1);
        verify(orderService).cancelTimeoutOrder(timeoutOrder2);
    }

    @Test
    void cancelTimeoutOrders_shouldUseConfiguredTimeoutMinutes() {
        // Given: 设置超时时间为 60 分钟
        ReflectionTestUtils.setField(orderTimeoutJob, "timeoutMinutes", 60);
        when(orderService.findTimeoutOrders(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        // When: 执行定时任务
        orderTimeoutJob.cancelTimeoutOrders();

        // Then: 应该使用配置的超时时间查询
        verify(orderService).findTimeoutOrders(any(LocalDateTime.class));
    }

    @Test
    void cancelTimeoutOrders_shouldHandleSingleTimeoutOrder() {
        // Given: 只有一个超时订单
        List<Order> timeoutOrders = Collections.singletonList(timeoutOrder1);
        when(orderService.findTimeoutOrders(any(LocalDateTime.class))).thenReturn(timeoutOrders);
        doNothing().when(orderService).cancelTimeoutOrder(timeoutOrder1);

        // When: 执行定时任务
        orderTimeoutJob.cancelTimeoutOrders();

        // Then: 应该取消该订单
        verify(orderService).findTimeoutOrders(any(LocalDateTime.class));
        verify(orderService).cancelTimeoutOrder(timeoutOrder1);
    }
}
