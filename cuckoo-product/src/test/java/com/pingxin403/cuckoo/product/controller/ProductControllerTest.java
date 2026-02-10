package com.pingxin403.cuckoo.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingxin403.cuckoo.common.exception.GlobalExceptionHandler;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.product.dto.CreateProductRequest;
import com.pingxin403.cuckoo.product.dto.ProductDTO;
import com.pingxin403.cuckoo.product.service.ProductService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProductController 单元测试（Standalone MockMvc）
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        // Register Jackson JavaTimeModule for LocalDateTime serialization
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ========== CreateProduct Tests ==========

    @Test
    @DisplayName("POST /api/products - should return 201 with ProductDTO")
    void createProduct_success() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "Test Product", new BigDecimal("99.99"), "A test product");
        ProductDTO productDTO = ProductDTO.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .description("A test product")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(productDTO);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andExpect(jsonPath("$.description").value("A test product"));
    }

    // ========== GetProductById Tests ==========

    @Test
    @DisplayName("GET /api/products/{id} - should return 200 with ProductDTO")
    void getProductById_success() throws Exception {
        ProductDTO productDTO = ProductDTO.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .description("A test product")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productService.getProductById(eq(1L))).thenReturn(productDTO);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andExpect(jsonPath("$.description").value("A test product"));
    }

    @Test
    @DisplayName("GET /api/products/{id} - should return 404 when product not found")
    void getProductById_notFound() throws Exception {
        when(productService.getProductById(eq(999L)))
                .thenThrow(new ResourceNotFoundException("Product", 999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ========== GetAllProducts Tests ==========

    @Test
    @DisplayName("GET /api/products - should return 200 with product list")
    void getAllProducts_success() throws Exception {
        ProductDTO product1 = ProductDTO.builder()
                .id(1L)
                .name("Product 1")
                .price(new BigDecimal("10.00"))
                .description("First product")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ProductDTO product2 = ProductDTO.builder()
                .id(2L)
                .name("Product 2")
                .price(new BigDecimal("20.00"))
                .description("Second product")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productService.getAllProducts()).thenReturn(List.of(product1, product2));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Product 1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Product 2"));
    }

    @Test
    @DisplayName("GET /api/products - should return 200 with empty list when no products")
    void getAllProducts_empty() throws Exception {
        when(productService.getAllProducts()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
