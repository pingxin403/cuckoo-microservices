package com.pingxin403.cuckoo.product.service;

import com.pingxin403.cuckoo.common.cache.BloomFilterService;
import com.pingxin403.cuckoo.common.cache.MultiLevelCacheManager;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.product.dto.CreateProductRequest;
import com.pingxin403.cuckoo.product.dto.ProductDTO;
import com.pingxin403.cuckoo.product.entity.Product;
import com.pingxin403.cuckoo.product.mapper.ProductMapper;
import com.pingxin403.cuckoo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务
 * 提供商品创建、查询和列表功能
 * 使用多级缓存策略（本地缓存 + Redis）
 * 实现缓存穿透、雪崩、击穿防护
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MultiLevelCacheManager cacheManager;
    private final BloomFilterService bloomFilterService;
    private final RedissonClient redissonClient;
    private final ProductMapper productMapper;

    private static final String CACHE_KEY_PREFIX = "product:";
    private static final String LOCK_KEY_PREFIX = "lock:product:";
    private static final long CACHE_TTL_MINUTES = 60;

    /**
     * 创建商品
     * 创建成功后将商品 ID 添加到布隆过滤器
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

        // 将商品 ID 添加到布隆过滤器
        bloomFilterService.add(String.valueOf(savedProduct.getId()));
        
        return productMapper.toDTO(savedProduct);
    }

    /**
     * 根据 ID 查询商品
     * 实现多级缓存策略 + 缓存穿透/雪崩/击穿防护：
     * 1. 布隆过滤器检查（防止缓存穿透）
     * 2. 查询多级缓存（本地缓存 -> Redis）
     * 3. 分布式锁防止缓存击穿
     * 4. 随机 TTL 防止缓存雪崩
     */
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        
        // 1. 布隆过滤器检查，防止缓存穿透
        if (!bloomFilterService.mightContain(String.valueOf(id))) {
            log.warn("Product not found in bloom filter: id={}", id);
            throw new ResourceNotFoundException("Product", id);
        }
        
        // 2. 查询多级缓存
        ProductDTO cachedProduct = cacheManager.get(cacheKey, ProductDTO.class);
        if (cachedProduct != null) {
            log.debug("Cache hit for product: id={}", id);
            return cachedProduct;
        }
        
        // 3. 缓存未命中，使用分布式锁防止缓存击穿
        String lockKey = LOCK_KEY_PREFIX + id;
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // 尝试获取锁，最多等待 3 秒，锁持有时间最多 10 秒
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                try {
                    // 双重检查：获取锁后再次查询缓存
                    cachedProduct = cacheManager.get(cacheKey, ProductDTO.class);
                    if (cachedProduct != null) {
                        log.debug("Cache hit after lock acquisition: id={}", id);
                        return cachedProduct;
                    }
                    
                    // 查询数据库
                    log.debug("Cache miss for product: id={}, querying database", id);
                    Product product = productRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
                    
                    ProductDTO productDTO = productMapper.toDTO(product);
                    
                    // 写入缓存，使用随机 TTL 防止缓存雪崩
                    Duration ttl = Duration.ofMinutes(CACHE_TTL_MINUTES + ThreadLocalRandom.current().nextInt(10));
                    cacheManager.put(cacheKey, productDTO, ttl);
                    log.debug("Product cached: id={}, ttl={}", id, ttl);
                    
                    return productDTO;
                } finally {
                    lock.unlock();
                }
            } else {
                // 获取锁失败，等待后重试
                log.warn("Failed to acquire lock for product: id={}, retrying...", id);
                Thread.sleep(100);
                return getProductById(id);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock", e);
        }
    }

    /**
     * 查询所有商品列表
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productMapper.toDTOList(productRepository.findAll());
    }

    /**
     * 更新商品
     * 实现缓存更新策略：
     * 1. 先更新数据库
     * 2. 再删除缓存（Cache-Aside Pattern）
     * 3. 通过 Redis Pub/Sub 通知其他实例删除本地缓存
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
        
        // 2. 再删除缓存（同时删除本地缓存和 Redis，并通知其他实例）
        String cacheKey = CACHE_KEY_PREFIX + id;
        cacheManager.evict(cacheKey);
        log.debug("Product cache evicted: id={}", id);
        
        return productMapper.toDTO(updatedProduct);
    }
}
