package com.pingxin403.cuckoo.product.service;

import com.pingxin403.cuckoo.common.cache.BloomFilterService;
import com.pingxin403.cuckoo.common.cache.MultiLevelCacheManager;
import com.pingxin403.cuckoo.common.health.WarmupService;
import com.pingxin403.cuckoo.product.dto.ProductDTO;
import com.pingxin403.cuckoo.product.entity.Product;
import com.pingxin403.cuckoo.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商品服务预热逻辑
 * 在服务启动时执行，确保服务在接收流量前完成初始化
 */
@Slf4j
@Component
public class ProductWarmupService implements WarmupService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired(required = false)
    private MultiLevelCacheManager cacheManager;
    
    @Autowired(required = false)
    private BloomFilterService bloomFilterService;
    
    @Override
    public void performWarmup() throws Exception {
        log.info("Starting product service warmup...");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 预热数据库连接池
            warmupDatabase();
            
            // 2. 初始化布隆过滤器
            if (bloomFilterService != null) {
                initializeBloomFilter();
            }
            
            // 3. 加载热点商品数据到缓存
            if (cacheManager != null) {
                loadHotProducts();
            }
            
            // 4. 触发 JVM 类加载和 JIT 编译
            triggerClassLoading();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Product service warmup completed in {} ms", duration);
            
        } catch (Exception e) {
            log.error("Product service warmup failed", e);
            throw e;
        }
    }
    
    /**
     * 预热数据库连接池
     * 执行简单查询以初始化连接
     */
    private void warmupDatabase() {
        try {
            productRepository.count();
            log.info("Database connection pool warmed up");
        } catch (Exception e) {
            log.error("Failed to warmup database connection pool", e);
            throw e;
        }
    }
    
    /**
     * 初始化布隆过滤器
     * 将所有商品 ID 添加到布隆过滤器，防止缓存穿透
     */
    private void initializeBloomFilter() {
        try {
            log.info("Initializing bloom filter...");
            
            // 清空旧的布隆过滤器
            bloomFilterService.clear();
            
            // 分批加载所有商品 ID
            int pageSize = 1000;
            int pageNumber = 0;
            int totalAdded = 0;
            
            while (true) {
                List<Product> products = productRepository.findAll(PageRequest.of(pageNumber, pageSize)).getContent();
                if (products.isEmpty()) {
                    break;
                }
                
                for (Product product : products) {
                    bloomFilterService.add(String.valueOf(product.getId()));
                    totalAdded++;
                }
                
                pageNumber++;
            }
            
            log.info("Bloom filter initialized with {} product IDs", totalAdded);
        } catch (Exception e) {
            log.error("Failed to initialize bloom filter", e);
            // 不抛出异常，允许服务继续启动
        }
    }
    
    /**
     * 加载热点商品数据到缓存
     * 预加载热门商品，减少冷启动时的数据库压力
     */
    private void loadHotProducts() {
        try {
            // 加载前 100 个商品到缓存（假设按 ID 排序）
            List<Product> hotProducts = productRepository.findAll(PageRequest.of(0, 100)).getContent();
            
            int loadedCount = 0;
            for (Product product : hotProducts) {
                String cacheKey = "product:" + product.getId();
                ProductDTO productDTO = toDTO(product);
                
                // 使用随机 TTL 防止缓存雪崩
                Duration ttl = Duration.ofMinutes(60 + ThreadLocalRandom.current().nextInt(10));
                cacheManager.put(cacheKey, productDTO, ttl);
                loadedCount++;
            }
            
            log.info("Loaded {} hot products into multi-level cache", loadedCount);
        } catch (Exception e) {
            log.error("Failed to load hot products", e);
            // 不抛出异常，允许服务继续启动
        }
    }
    
    /**
     * 触发 JVM 类加载和 JIT 编译
     * 执行核心业务逻辑以触发类加载
     */
    private void triggerClassLoading() {
        try {
            // 尝试查询一个商品，触发相关类的加载
            productRepository.findById(1L).orElse(null);
            log.info("JVM class loading triggered");
        } catch (Exception e) {
            // 忽略错误，只是为了触发类加载
            log.debug("Class loading trigger completed (error ignored): {}", e.getMessage());
        }
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
