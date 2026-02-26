package com.pingxin403.cuckoo.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.payment.dto.CreatePaymentRequest;
import com.pingxin403.cuckoo.payment.entity.Payment;
import com.pingxin403.cuckoo.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.pingxin403.cuckoo.payment.TestPaymentApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrderId(100L);
        testPayment.setUserId(1L);
        testPayment.setAmount(new BigDecimal("99.99"));
        testPayment.setStatus(Payment.PaymentStatus.PENDING);
    }

    @Test
    void createPayment_shouldReturn200() throws Exception {
        when(paymentService.createPayment(any(Payment.class))).thenReturn(testPayment);

        CreatePaymentRequest request = new CreatePaymentRequest(100L, 1L, new BigDecimal("99.99"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void confirmPayment_shouldReturn200() throws Exception {
        testPayment.setStatus(Payment.PaymentStatus.SUCCESS);
        when(paymentService.confirmPayment(1L)).thenReturn(testPayment);

        mockMvc.perform(post("/api/payments/1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void failPayment_shouldReturn200() throws Exception {
        testPayment.setStatus(Payment.PaymentStatus.FAILED);
        when(paymentService.failPayment(anyLong(), any())).thenReturn(testPayment);

        mockMvc.perform(post("/api/payments/1/fail")
                        .param("reason", "Insufficient funds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void getPayment_shouldReturn200() throws Exception {
        when(paymentService.getPaymentById(1L)).thenReturn(testPayment);

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100));
    }

    @Test
    void getPayment_shouldReturn404WhenNotFound() throws Exception {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new ResourceNotFoundException("Payment not found with id: 999"));

        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound());
    }
}
