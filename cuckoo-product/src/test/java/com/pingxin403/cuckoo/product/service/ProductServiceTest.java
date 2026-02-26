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
import java.util.concurrent.TimeUnit;

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
    private com.pingxin403.cuckoo.common.cache.MultiLevelCacheManager multiLevelCacheManager;

    @Mock
    private com.pingxin403.cuckoo.common.cache.BloomFilterService bloomFilterService;

    @Mock
    private org.redisson.api.RedissonClient redissonClient;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Mock
    private org.springframework.data.redis.core.ValueOperations<String, Object> valueOperations;

    @Mock
    private com.pingxin403.cuckoo.product.mapper.ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductDTO testProductDTO;

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
        
        testProductDTO = ProductDTO.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .description("A test product description")
                .createdAt(testProduct.getCreatedAt())
                .updatedAt(testProduct.getUpdatedAt())
                .build();
        
        // Mock RedisTemplate behavior
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Mock BloomFilterService to return true (item might exist)
        when(bloomFilterService.mightContain(anyString())).thenReturn(true);
        
        // Mock MultiLevelCacheManager to return null (cache miss)
        when(multiLevelCacheManager.get(anyString(), any())).thenReturn(null);
        
        // Mock ProductMapper
        when(productMapper.toDTO(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            if (product == null) return null;
            return ProductDTO.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .price(product.getPrice())
                    .description(product.getDescription())
                    .createdAt(product.getCreatedAt())
                    .updatedAt(product.getUpdatedAt())
                    .build();
        });
        
        when(productMapper.toDTOList(anyList())).thenAnswer(invocation -> {
            List<Product> products = invocation.getArgument(0);
            if (products == null) return null;
            return products.stream()
                    .map(product -> ProductDTO.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .price(product.getPrice())
                            .description(product.getDescription())
                            .createdAt(product.getCreatedAt())
                            .updatedAt(product.getUpdatedAt())
                            .build())
                    .toList();
        });
        
        // Mock RedissonClient lock behavior
        org.redisson.api.RLock lock = mock(org.redisson.api.RLock.class);
        try {
            when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        } catch (InterruptedException e) {
            // Won't happen in mock
        }
        when(redissonClient.getLock(anyString())).thenReturn(lock);
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
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        ProductDTO result = productService.getProductById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(result.getDescription()).isEqualTo("A test product description");
        
        // Verify cache was checked twice (initial check + double-check after lock) and updated
        verify(multiLevelCacheManager, times(2)).get(eq("product:1"), eq(ProductDTO.class));
        verify(multiLevelCacheManager).put(eq("product:1"), any(ProductDTO.class), any());
    }

    @Test
    @DisplayName("getProductById - should throw ResourceNotFoundException when product not found")
    void getProductById_notFound() {
        // Mock bloom filter to return false (item doesn't exist)
        when(bloomFilterService.mightContain("999")).thenReturn(false);

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
