package com.pingxin403.cuckoo.common.exception;

import com.pingxin403.cuckoo.common.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GlobalExceptionHandler 单元测试
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleResourceNotFound_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", 1L);

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_FOUND", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("User"));
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleDuplicateResource_shouldReturn409() {
        DuplicateResourceException ex = new DuplicateResourceException("User", "email", "test@test.com");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicateResource(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("CONFLICT", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("User"));
    }

    @Test
    void handleInsufficientStock_shouldReturn409() {
        InsufficientStockException ex = new InsufficientStockException(100L, 10, 5);

        ResponseEntity<ErrorResponse> response = handler.handleInsufficientStock(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INSUFFICIENT_STOCK", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("100"));
    }

    @Test
    void handleBusinessException_shouldReturn400() {
        BusinessException ex = new BusinessException("Invalid operation");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BAD_REQUEST", response.getBody().getError());
        assertEquals("Invalid operation", response.getBody().getMessage());
    }

    @Test
    void handleGenericException_shouldReturn500() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getError());
        assertEquals("Internal server error", response.getBody().getMessage());
    }
}
