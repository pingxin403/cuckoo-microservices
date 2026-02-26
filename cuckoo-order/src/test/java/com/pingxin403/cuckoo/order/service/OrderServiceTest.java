package com.pingxin403.cuckoo.order.service;

import com.pingxin403.cuckoo.common.event.EventPublisher;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.order.client.InventoryClient;
import com.pingxin403.cuckoo.order.client.PaymentClient;
import com.pingxin403.cuckoo.order.client.ProductClient;
import com.pingxin403.cuckoo.order.dto.*;
import com.pingxin403.cuckoo.order.entity.Order;
import com.pingxin403.cuckoo.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private com.pingxin403.cuckoo.common.event.EventPublisherUtil eventPublisher;

    @Mock
    private com.pingxin403.cuckoo.common.message.LocalMessageService localMessageService;

    @Mock
    private com.pingxin403.cuckoo.order.saga.SagaOrchestrator sagaOrchestrator;

    @Mock
    private com.pingxin403.cuckoo.order.saga.OrderSagaDefinition orderSagaDefinition;

    @Mock
    private com.pingxin403.cuckoo.order.mapper.OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private ProductDTO testProduct;
    private PaymentDTO testPayment;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNo("ORD123456");
        testOrder.setUserId(1L);
        testOrder.setSkuId(100L);
        testOrder.setProductName("Test Product");
        testOrder.setQuantity(2);
        testOrder.setUnitPrice(new BigDecimal("50.00"));
        testOrder.setTotalAmount(new BigDecimal("100.00"));
        testOrder.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        testProduct = ProductDTO.builder()
                .id(100L)
                .name("Test Product")
                .price(new BigDecimal("50.00"))
                .description("Test Description")
                .build();

        testPayment = PaymentDTO.builder()
                .id(1L)
                .orderId(1L)
                .userId(1L)
                .amount(new BigDecimal("100.00"))
                .status("PENDING")
                .build();
    }

    @Test
    void createOrder_shouldCreateOrderSuccessfully() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 100L, 2);

        when(productClient.getProduct(100L)).thenReturn(testProduct);
        doNothing().when(inventoryClient).reserveInventory(any(ReserveInventoryRequest.class));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(paymentClient.createPayment(any(CreatePaymentRequest.class))).thenReturn(testPayment);
        
        // Mock OrderMapper
        OrderDTO expectedDTO = OrderDTO.builder()
                .id(testOrder.getId())
                .orderNo(testOrder.getOrderNo())
                .userId(testOrder.getUserId())
                .skuId(testOrder.getSkuId())
                .productName(testOrder.getProductName())
                .quantity(testOrder.getQuantity())
                .unitPrice(testOrder.getUnitPrice())
                .totalAmount(testOrder.getTotalAmount())
                .status(testOrder.getStatus().name())
                .build();
        when(orderMapper.toDTO(any(Order.class))).thenReturn(expectedDTO);

        OrderDTO result = orderService.createOrder(request);

        assertNotNull(result);
        assertEquals(testOrder.getId(), result.getId());
        assertEquals(testOrder.getOrderNo(), result.getOrderNo());
        assertEquals(Order.OrderStatus.PENDING_PAYMENT.name(), result.getStatus());
        verify(productClient).getProduct(100L);
        verify(inventoryClient).reserveInventory(any(ReserveInventoryRequest.class));
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(paymentClient).createPayment(any(CreatePaymentRequest.class));
    }

    @Test
    void createOrder_shouldThrowExceptionWhenProductNotFound() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 999L, 2);

        when(productClient.getProduct(999L)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.createOrder(request);
        });

        verify(productClient).getProduct(999L);
        verify(inventoryClient, never()).reserveInventory(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrder_shouldReturnOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        
        // Mock OrderMapper
        OrderDTO expectedDTO = OrderDTO.builder()
                .id(testOrder.getId())
                .orderNo(testOrder.getOrderNo())
                .userId(testOrder.getUserId())
                .skuId(testOrder.getSkuId())
                .productName(testOrder.getProductName())
                .quantity(testOrder.getQuantity())
                .unitPrice(testOrder.getUnitPrice())
                .totalAmount(testOrder.getTotalAmount())
                .status(testOrder.getStatus().name())
                .build();
        when(orderMapper.toDTO(any(Order.class))).thenReturn(expectedDTO);

        OrderDTO result = orderService.getOrder(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ORD123456", result.getOrderNo());
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrder_shouldThrowExceptionWhenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.getOrder(999L);
        });
    }

    @Test
    void getUserOrders_shouldReturnOrderList() {
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findByUserId(1L)).thenReturn(orders);
        
        // Mock OrderMapper
        OrderDTO expectedDTO = OrderDTO.builder()
                .id(testOrder.getId())
                .orderNo(testOrder.getOrderNo())
                .userId(testOrder.getUserId())
                .skuId(testOrder.getSkuId())
                .productName(testOrder.getProductName())
                .quantity(testOrder.getQuantity())
                .unitPrice(testOrder.getUnitPrice())
                .totalAmount(testOrder.getTotalAmount())
                .status(testOrder.getStatus().name())
                .build();
        when(orderMapper.toDTOList(anyList())).thenReturn(Arrays.asList(expectedDTO));

        List<OrderDTO> result = orderService.getUserOrders(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrder.getId(), result.get(0).getId());
        verify(orderRepository).findByUserId(1L);
    }

    @Test
    void cancelOrder_shouldCancelOrderAndPublishEvent() {
        // Ensure order is in PENDING_PAYMENT status
        testOrder.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        
        // Mock the save to return the cancelled order
        Order cancelledOrder = new Order();
        cancelledOrder.setId(testOrder.getId());
        cancelledOrder.setOrderNo(testOrder.getOrderNo());
        cancelledOrder.setUserId(testOrder.getUserId());
        cancelledOrder.setSkuId(testOrder.getSkuId());
        cancelledOrder.setProductName(testOrder.getProductName());
        cancelledOrder.setQuantity(testOrder.getQuantity());
        cancelledOrder.setUnitPrice(testOrder.getUnitPrice());
        cancelledOrder.setTotalAmount(testOrder.getTotalAmount());
        cancelledOrder.setStatus(Order.OrderStatus.CANCELLED);
        cancelledOrder.setCancelReason("用户主动取消");
        cancelledOrder.setCreatedAt(testOrder.getCreatedAt());
        cancelledOrder.setUpdatedAt(LocalDateTime.now());
        
        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);
        
        // Mock localMessageService
        doNothing().when(localMessageService).saveMessage(any());
        doNothing().when(localMessageService).markAsSent(anyString());
        
        // Mock eventPublisher to return a completed future with proper type
        org.springframework.kafka.support.SendResult<String, com.pingxin403.cuckoo.common.event.DomainEvent> mockSendResult = 
            mock(org.springframework.kafka.support.SendResult.class);
        when(eventPublisher.publish(anyString(), anyString(), any()))
            .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockSendResult));
        
        // Mock OrderMapper
        OrderDTO expectedDTO = OrderDTO.builder()
                .id(cancelledOrder.getId())
                .orderNo(cancelledOrder.getOrderNo())
                .userId(cancelledOrder.getUserId())
                .skuId(cancelledOrder.getSkuId())
                .productName(cancelledOrder.getProductName())
                .quantity(cancelledOrder.getQuantity())
                .unitPrice(cancelledOrder.getUnitPrice())
                .totalAmount(cancelledOrder.getTotalAmount())
                .status(cancelledOrder.getStatus().name())
                .cancelReason(cancelledOrder.getCancelReason())
                .build();
        when(orderMapper.toDTO(any(Order.class))).thenReturn(expectedDTO);

        OrderDTO result = orderService.cancelOrder(1L);

        assertEquals(Order.OrderStatus.CANCELLED.name(), result.getStatus());
        assertEquals("用户主动取消", result.getCancelReason());
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
        verify(localMessageService).saveMessage(any());
        verify(eventPublisher).publish(eq("order-events"), anyString(), any());
    }

    @Test
    void cancelOrder_shouldThrowExceptionWhenOrderNotPending() {
        testOrder.setStatus(Order.OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThrows(IllegalStateException.class, () -> {
            orderService.cancelOrder(1L);
        });

        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publish(anyString(), anyString(), any());
    }

    @Test
    void updateOrderToPaid_shouldUpdateStatus() {
        // Ensure order is in PENDING_PAYMENT status
        testOrder.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        
        // Mock the save to return the paid order
        Order paidOrder = new Order();
        paidOrder.setId(testOrder.getId());
        paidOrder.setOrderNo(testOrder.getOrderNo());
        paidOrder.setUserId(testOrder.getUserId());
        paidOrder.setSkuId(testOrder.getSkuId());
        paidOrder.setProductName(testOrder.getProductName());
        paidOrder.setQuantity(testOrder.getQuantity());
        paidOrder.setUnitPrice(testOrder.getUnitPrice());
        paidOrder.setTotalAmount(testOrder.getTotalAmount());
        paidOrder.setStatus(Order.OrderStatus.PAID);
        paidOrder.setPaymentId(1L);
        paidOrder.setCreatedAt(testOrder.getCreatedAt());
        paidOrder.setUpdatedAt(LocalDateTime.now());
        
        when(orderRepository.save(any(Order.class))).thenReturn(paidOrder);

        orderService.updateOrderToPaid(1L, 1L);

        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrderToPaid_shouldSkipWhenOrderNotPending() {
        testOrder.setStatus(Order.OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        orderService.updateOrderToPaid(1L, 1L);

        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelTimeoutOrder_shouldCancelAndPublishEvent() {
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        
        // Mock localMessageService
        doNothing().when(localMessageService).saveMessage(any());
        doNothing().when(localMessageService).markAsSent(anyString());
        
        // Mock eventPublisher to return a completed future with proper type
        org.springframework.kafka.support.SendResult<String, com.pingxin403.cuckoo.common.event.DomainEvent> mockSendResult = 
            mock(org.springframework.kafka.support.SendResult.class);
        when(eventPublisher.publish(anyString(), anyString(), any()))
            .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(mockSendResult));

        orderService.cancelTimeoutOrder(testOrder);

        verify(orderRepository).save(any(Order.class));
        verify(localMessageService).saveMessage(any());
        verify(eventPublisher).publish(eq("order-events"), anyString(), any());
    }

    @Test
    void findTimeoutOrders_shouldReturnTimeoutOrders() {
        LocalDateTime timeoutBefore = LocalDateTime.now().minusMinutes(30);
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findByStatusAndCreatedAtBefore(
                Order.OrderStatus.PENDING_PAYMENT, timeoutBefore))
                .thenReturn(orders);

        List<Order> result = orderService.findTimeoutOrders(timeoutBefore);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(orderRepository).findByStatusAndCreatedAtBefore(
                Order.OrderStatus.PENDING_PAYMENT, timeoutBefore);
    }
}
