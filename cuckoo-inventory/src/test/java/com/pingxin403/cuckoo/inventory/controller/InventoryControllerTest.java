package com.pingxin403.cuckoo.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingxin403.cuckoo.common.exception.GlobalExceptionHandler;
import com.pingxin403.cuckoo.common.exception.InsufficientStockException;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.inventory.dto.InitInventoryRequest;
import com.pingxin403.cuckoo.inventory.dto.InventoryDTO;
import com.pingxin403.cuckoo.inventory.dto.InventoryOperationRequest;
import com.pingxin403.cuckoo.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * InventoryController 单元测试（Standalone MockMvc）
 */
@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ========== InitInventory Tests ==========

    @Test
    @DisplayName("POST /api/inventory/init - should return 201 with InventoryDTO")
    void initInventory_success() throws Exception {
        InitInventoryRequest request = new InitInventoryRequest(100L, 50);
        InventoryDTO inventoryDTO = InventoryDTO.builder()
                .id(1L)
                .skuId(100L)
                .totalStock(50)
                .availableStock(50)
                .reservedStock(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(inventoryService.initInventory(any(InitInventoryRequest.class))).thenReturn(inventoryDTO);

        mockMvc.perform(post("/api/inventory/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.skuId").value(100))
                .andExpect(jsonPath("$.totalStock").value(50))
                .andExpect(jsonPath("$.availableStock").value(50))
                .andExpect(jsonPath("$.reservedStock").value(0));
    }

    // ========== ReserveStock Tests ==========

    @Test
    @DisplayName("POST /api/inventory/reserve - should return 200 on success")
    void reserveStock_success() throws Exception {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 10, "order-001");

        doNothing().when(inventoryService).reserveStock(any(InventoryOperationRequest.class));

        mockMvc.perform(post("/api/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/inventory/reserve - should return 409 when stock insufficient")
    void reserveStock_insufficientStock() throws Exception {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 100, "order-002");

        doThrow(new InsufficientStockException(100L, 100, 10))
                .when(inventoryService).reserveStock(any(InventoryOperationRequest.class));

        mockMvc.perform(post("/api/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"));
    }

    // ========== DeductStock Tests ==========

    @Test
    @DisplayName("POST /api/inventory/deduct - should return 200 on success")
    void deductStock_success() throws Exception {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 10, "order-001");

        doNothing().when(inventoryService).deductStock(any(InventoryOperationRequest.class));

        mockMvc.perform(post("/api/inventory/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ========== ReleaseStock Tests ==========

    @Test
    @DisplayName("POST /api/inventory/release - should return 200 on success")
    void releaseStock_success() throws Exception {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 10, "order-001");

        doNothing().when(inventoryService).releaseStock(any(InventoryOperationRequest.class));

        mockMvc.perform(post("/api/inventory/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ========== GetInventory Tests ==========

    @Test
    @DisplayName("GET /api/inventory/{skuId} - should return 200 with InventoryDTO")
    void getInventory_success() throws Exception {
        InventoryDTO inventoryDTO = InventoryDTO.builder()
                .id(1L)
                .skuId(100L)
                .totalStock(100)
                .availableStock(80)
                .reservedStock(20)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(inventoryService.getInventoryBySkuId(eq(100L))).thenReturn(inventoryDTO);

        mockMvc.perform(get("/api/inventory/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuId").value(100))
                .andExpect(jsonPath("$.totalStock").value(100))
                .andExpect(jsonPath("$.availableStock").value(80))
                .andExpect(jsonPath("$.reservedStock").value(20));
    }

    @Test
    @DisplayName("GET /api/inventory/{skuId} - should return 404 when inventory not found")
    void getInventory_notFound() throws Exception {
        when(inventoryService.getInventoryBySkuId(eq(999L)))
                .thenThrow(new ResourceNotFoundException("Inventory", 999L));

        mockMvc.perform(get("/api/inventory/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
