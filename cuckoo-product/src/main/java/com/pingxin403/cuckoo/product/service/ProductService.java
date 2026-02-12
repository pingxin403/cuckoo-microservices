package com.pingxin403.cuckoo.product.service;

import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.product.dto.CreateProductRequest;
import com.pingxin403.cuckoo.product.dto.ProductDTO;
import com.pingxin403.cuckoo.product.entity.Product;
import com.pingxin403.cuckoo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品服务
 * 提供商品创建、查询和列表功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "product:";
    private static final long CACHE_TTL_MINUTES = 30;

    /**
     * 创建商品
     */
    @Transactional
    public ProductDTO createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .description(request.getDescription())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully: name={}, id={}", savedProduct.getName(), savedProduct.getId());

        return toDTO(savedProduct);
    }

    /**
     * 根据 ID 查询商品
     * 实现 Cache-Aside Pattern：
     * 1. 先查询缓存
     * 2. 缓存命中则直接返回
     * 3. 缓存未命中则查询数据库
     * 4. 将查询结果写入缓存（TTL 30分钟）
     */
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        
        // 1. 先查询缓存
        ProductDTO cachedProduct = (ProductDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            log.debug("Cache hit for product: id={}", id);
            return cachedProduct;
        }
        
        // 2. 缓存未命中，查询数据库
        log.debug("Cache miss for product: id={}, querying database", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        
        ProductDTO productDTO = toDTO(product);
        
        // 3. 将查询结果写入缓存，设置 TTL 为 30 分钟
        redisTemplate.opsForValue().set(cacheKey, productDTO, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("Product cached: id={}, ttl={}min", id, CACHE_TTL_MINUTES);
        
        return productDTO;
    }

    /**
     * 查询所有商品列表
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 更新商品
     * 实现缓存更新策略：
     * 1. 先更新数据库
     * 2. 再删除缓存（Cache-Aside Pattern）
     */
    @Transactional
    public ProductDTO updateProduct(Long id, com.pingxin403.cuckoo.product.dto.UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        
        // 更新商品信息
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        
        // 1. 先更新数据库
        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully: id={}", id);
        
        // 2. 再删除缓存
        String cacheKey = CACHE_KEY_PREFIX + id;
        redisTemplate.delete(cacheKey);
        log.debug("Product cache deleted: id={}", id);
        
        return toDTO(updatedProduct);
    }

    /**
     * 将 Product 实体转换为 ProductDTO
     */
    private ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .description(product.getDescription())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
