package com.pingxin403.cuckoo.product.service;

import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.product.dto.CreateProductRequest;
import com.pingxin403.cuckoo.product.dto.ProductDTO;
import com.pingxin403.cuckoo.product.entity.Product;
import com.pingxin403.cuckoo.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProductService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Mock
    private org.springframework.data.redis.core.ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .description("A test product description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // Mock RedisTemplate behavior
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ========== CreateProduct Tests ==========

    @Test
    @DisplayName("createProduct - should create product and return ProductDTO")
    void createProduct_success() {
        CreateProductRequest request = new CreateProductRequest(
                "Test Product", new BigDecimal("99.99"), "A test product description");

        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductDTO result = productService.createProduct(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(result.getDescription()).isEqualTo("A test product description");

        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct - should handle product with null description")
    void createProduct_nullDescription() {
        CreateProductRequest request = new CreateProductRequest(
                "Simple Product", new BigDecimal("10.00"), null);

        Product savedProduct = Product.builder()
                .id(2L)
                .name("Simple Product")
                .price(new BigDecimal("10.00"))
                .description(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductDTO result = productService.createProduct(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Simple Product");
        assertThat(result.getDescription()).isNull();
    }

    // ========== GetProductById Tests ==========

    @Test
    @DisplayName("getProductById - should return ProductDTO when product exists")
    void getProductById_success() {
        when(valueOperations.get("product:1")).thenReturn(null); // Cache miss
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        ProductDTO result = productService.getProductById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(result.getDescription()).isEqualTo("A test product description");
        
        verify(valueOperations).set(eq("product:1"), any(ProductDTO.class), eq(30L), eq(java.util.concurrent.TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("getProductById - should throw ResourceNotFoundException when product not found")
    void getProductById_notFound() {
        when(valueOperations.get("product:999")).thenReturn(null); // Cache miss
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    // ========== GetAllProducts Tests ==========

    @Test
    @DisplayName("getAllProducts - should return list of ProductDTOs")
    void getAllProducts_success() {
        Product product2 = Product.builder()
                .id(2L)
                .name("Another Product")
                .price(new BigDecimal("49.99"))
                .description("Another description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(productRepository.findAll()).thenReturn(List.of(testProduct, product2));

        List<ProductDTO> result = productService.getAllProducts();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Test Product");
        assertThat(result.get(1).getName()).isEqualTo("Another Product");
    }

    @Test
    @DisplayName("getAllProducts - should return empty list when no products exist")
    void getAllProducts_empty() {
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        List<ProductDTO> result = productService.getAllProducts();

        assertThat(result).isEmpty();
    }
}
