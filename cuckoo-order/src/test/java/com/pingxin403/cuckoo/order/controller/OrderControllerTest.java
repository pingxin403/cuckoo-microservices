package com.pingxin403.cuckoo.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.order.dto.CreateOrderRequest;
import com.pingxin403.cuckoo.order.dto.OrderDTO;
import com.pingxin403.cuckoo.order.service.OrderService;
import com.pingxin403.cuckoo.order.service.OrderQueryService;
import com.pingxin403.cuckoo.order.TestOrderApplication;
import com.pingxin403.cuckoo.order.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = TestOrderApplication.class,
        properties = {
                "spring.kafka.bootstrap-servers=",
                "spring.cloud.openfeign.client.config.default.url=http://localhost"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderQueryService orderQueryService;

    @MockBean
    private com.pingxin403.cuckoo.common.audit.AuditLogService auditLogService;

    private OrderDTO testOrderDTO;

    @BeforeEach
    void setUp() {
        // Configure AuditLogService mock
        when(auditLogService.buildAuditLog(any(), any(), anyString())).thenAnswer(invocation -> {
            return com.pingxin403.cuckoo.common.audit.AuditLog.builder()
                    .operationType((com.pingxin403.cuckoo.common.audit.AuditLog.OperationType) invocation.getArgument(0))
                    .userId((Long) invocation.getArgument(1))
                    .username(invocation.getArgument(2));
        });
        
        testOrderDTO = OrderDTO.builder()
                .id(1L)
                .orderNo("ORD123456")
                .userId(1L)
                .skuId(100L)
                .productName("Test Product")
                .quantity(2)
                .unitPrice(new BigDecimal("50.00"))
                .totalAmount(new BigDecimal("100.00"))
                .status("PENDING_PAYMENT")
                .cancelReason(null)
                .paymentId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createOrder_shouldReturnCreatedOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(1L, 100L, 2);
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(testOrderDTO);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())  // Changed from isOk() to isCreated() for HTTP 201
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNo").value("ORD123456"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount").value(100.00));
    }

    @Test
    void getOrder_shouldReturnOrder() throws Exception {
        when(orderService.getOrder(1L)).thenReturn(testOrderDTO);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNo").value("ORD123456"));
    }

    @Test
    void getOrder_shouldReturn404WhenNotFound() throws Exception {
        when(orderService.getOrder(999L)).thenThrow(new ResourceNotFoundException("订单不存在"));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserOrders_shouldReturnOrderList() throws Exception {
        List<OrderDTO> orders = Arrays.asList(testOrderDTO);
        when(orderQueryService.getUserOrders(1L)).thenReturn(orders);

        mockMvc.perform(get("/api/orders/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].orderNo").value("ORD123456"));
    }

    @Test
    void cancelOrder_shouldReturnCancelledOrder() throws Exception {
        testOrderDTO.setStatus("CANCELLED");
        testOrderDTO.setCancelReason("用户主动取消");
        when(orderService.cancelOrder(1L)).thenReturn(testOrderDTO);

        mockMvc.perform(put("/api/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("用户主动取消"));
    }

    @Test
    void cancelOrder_shouldReturn404WhenNotFound() throws Exception {
        when(orderService.cancelOrder(999L)).thenThrow(new ResourceNotFoundException("订单不存在"));

        mockMvc.perform(put("/api/orders/999/cancel"))
                .andExpect(status().isNotFound());
    }
}
