package com.pingxin403.cuckoo.product.service;

import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.product.dto.CreateProductRequest;
import com.pingxin403.cuckoo.product.dto.ProductDTO;
import com.pingxin403.cuckoo.product.entity.Product;
import com.pingxin403.cuckoo.product.repository.ProductRepository;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * ProductService 属性测试
 * 使用 jqwik 框架验证商品服务的正确性属性
 * 
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
@Import(com.pingxin403.cuckoo.product.config.TestConfig.class)
class ProductServicePropertyTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    /**
     * 属性测试：商品创建 - 验证商品数据完整性
     * 
     * 对于任意有效的商品创建请求，创建的商品应该包含所有必需的字段，
     * 并且字段值应该与请求中的值一致
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Product creation maintains data integrity")
    @Transactional
    void productCreation_maintainsDataIntegrity(
            @ForAll("validCreateProductRequests") CreateProductRequest request) {
        
        // Clean database
        productRepository.deleteAll();
        
        // Act: Create product
        ProductDTO createdProduct = productService.createProduct(request);
        
        // Assert: Product should be created with all required fields
        assertThat(createdProduct).isNotNull();
        assertThat(createdProduct.getId()).isNotNull();
        assertThat(createdProduct.getName()).isEqualTo(request.getName());
        assertThat(createdProduct.getPrice()).isEqualByComparingTo(request.getPrice());
        assertThat(createdProduct.getDescription()).isEqualTo(request.getDescription());
        assertThat(createdProduct.getCreatedAt()).isNotNull();
        assertThat(createdProduct.getUpdatedAt()).isNotNull();
        
        // Assert: Product should be persisted in database
        Product savedProduct = productRepository.findById(createdProduct.getId()).orElseThrow();
        assertThat(savedProduct.getName()).isEqualTo(request.getName());
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(request.getPrice());
        assertThat(savedProduct.getDescription()).isEqualTo(request.getDescription());
    }

    /**
     * 属性测试：商品创建 - 验证价格非负
     * 
     * 对于任意有效的商品创建请求，创建的商品价格应该是非负数
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Product creation ensures non-negative price")
    @Transactional
    void productCreation_ensuresNonNegativePrice(
            @ForAll("validCreateProductRequests") CreateProductRequest request) {
        
        // Clean database
        productRepository.deleteAll();
        
        // Act: Create product
        ProductDTO createdProduct = productService.createProduct(request);
        
        // Assert: Price should be non-negative
        assertThat(createdProduct.getPrice()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        
        // Assert: Price should have correct scale (2 decimal places)
        assertThat(createdProduct.getPrice().scale()).isLessThanOrEqualTo(2);
    }

    /**
     * 属性测试：商品创建 - 验证时间戳自动设置
     * 
     * 对于任意有效的商品创建请求，创建的商品应该自动设置 createdAt 和 updatedAt 时间戳
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Product creation automatically sets timestamps")
    @Transactional
    void productCreation_automaticallySetsTimestamps(
            @ForAll("validCreateProductRequests") CreateProductRequest request) {
        
        // Clean database
        productRepository.deleteAll();
        
        // Act: Create product
        ProductDTO createdProduct = productService.createProduct(request);
        
        // Assert: Timestamps should be set
        assertThat(createdProduct.getCreatedAt()).isNotNull();
        assertThat(createdProduct.getUpdatedAt()).isNotNull();
        
        // Assert: Both timestamps should be recent (within last minute)
        java.time.LocalDateTime oneMinuteAgo = java.time.LocalDateTime.now().minusMinutes(1);
        assertThat(createdProduct.getCreatedAt()).isAfterOrEqualTo(oneMinuteAgo);
        assertThat(createdProduct.getUpdatedAt()).isAfterOrEqualTo(oneMinuteAgo);
        
        // Assert: createdAt should not be after updatedAt
        assertThat(createdProduct.getCreatedAt()).isBeforeOrEqualTo(createdProduct.getUpdatedAt());
    }

    /**
     * 属性测试：商品查询 - 验证查询结果正确性
     * 
     * 对于任意创建的商品，通过 ID 查询应该返回正确的商品信息
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Product query returns correct product information")
    @Transactional
    void productQuery_returnsCorrectProductInformation(
            @ForAll("validCreateProductRequests") CreateProductRequest request) {
        
        // Clean database
        productRepository.deleteAll();
        
        // Arrange: Create product
        ProductDTO createdProduct = productService.createProduct(request);
        
        // Act: Query product by ID
        ProductDTO queriedProduct = productService.getProductById(createdProduct.getId());
        
        // Assert: Queried product should match created product
        assertThat(queriedProduct).isNotNull();
        assertThat(queriedProduct.getId()).isEqualTo(createdProduct.getId());
        assertThat(queriedProduct.getName()).isEqualTo(createdProduct.getName());
        assertThat(queriedProduct.getPrice()).isEqualByComparingTo(createdProduct.getPrice());
        assertThat(queriedProduct.getDescription()).isEqualTo(createdProduct.getDescription());
        assertThat(queriedProduct.getCreatedAt()).isEqualTo(createdProduct.getCreatedAt());
        assertThat(queriedProduct.getUpdatedAt()).isEqualTo(createdProduct.getUpdatedAt());
    }

    /**
     * 属性测试：商品查询 - 验证不存在的商品抛出异常
     * 
     * 对于任意不存在的商品 ID，查询应该抛出 ResourceNotFoundException
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Product query throws exception for non-existent product")
    @Transactional
    void productQuery_throwsExceptionForNonExistentProduct(
            @ForAll("positiveIds") Long nonExistentId) {
        
        // Clean database
        productRepository.deleteAll();
        
        // Assume: Product with this ID does not exist
        Assume.that(!productRepository.existsById(nonExistentId));
        
        // Act & Assert: Query should throw ResourceNotFoundException
        assertThatThrownBy(() -> productService.getProductById(nonExistentId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Product");
    }

    /**
     * 属性测试：商品创建 - 验证名称不为空
     * 
     * 对于任意有效的商品创建请求，创建的商品名称应该不为空
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("Product creation ensures non-empty name")
    @Transactional
    void productCreation_ensuresNonEmptyName(
            @ForAll("validCreateProductRequests") CreateProductRequest request) {
        
        // Clean database
        productRepository.deleteAll();
        
        // Act: Create product
        ProductDTO createdProduct = productService.createProduct(request);
        
        // Assert: Name should not be null or empty
        assertThat(createdProduct.getName()).isNotNull();
        assertThat(createdProduct.getName()).isNotEmpty();
        assertThat(createdProduct.getName()).isEqualTo(request.getName());
    }

    // ========== Data Generators ==========

    /**
     * 生成有效的商品创建请求
     * 
     * 商品名称：3-50 个字符
     * 价格：0.01 到 9999.99 之间的正数，最多 2 位小数
     * 描述：0-200 个字符
     */
    @Provide
    Arbitrary<CreateProductRequest> validCreateProductRequests() {
        Arbitrary<String> names = Arbitraries.strings()
            .withCharRange('a', 'z')
            .withCharRange('A', 'Z')
            .withChars(' ')
            .ofMinLength(3)
            .ofMaxLength(50);
        
        Arbitrary<BigDecimal> prices = Arbitraries.bigDecimals()
            .between(BigDecimal.valueOf(0.01), BigDecimal.valueOf(9999.99))
            .ofScale(2);
        
        Arbitrary<String> descriptions = Arbitraries.strings()
            .withCharRange('a', 'z')
            .withCharRange('A', 'Z')
            .withChars(' ', '.', ',', '!', '?')
            .ofMinLength(0)
            .ofMaxLength(200);
        
        return Combinators.combine(names, prices, descriptions)
            .as(CreateProductRequest::new);
    }

    /**
     * 生成正整数 ID
     */
    @Provide
    Arbitrary<Long> positiveIds() {
        return Arbitraries.longs().between(1L, 999999L);
    }
}
