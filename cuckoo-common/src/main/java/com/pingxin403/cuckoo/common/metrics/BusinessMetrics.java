package com.pingxin403.cuckoo.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 业务指标收集器
 * 提供统一的业务指标收集接口，用于 Prometheus 监控
 * 
 * 使用示例：
 * <pre>
 * // 记录订单创建
 * businessMetrics.recordOrderCreated("SUCCESS");
 * 
 * // 记录订单金额
 * businessMetrics.recordOrderAmount(new BigDecimal("99.99"));
 * 
 * // 记录支付操作
 * businessMetrics.recordPayment(true);
 * </pre>
 */
@Slf4j
@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;
    
    // 订单相关指标
    private final Counter orderCreatedCounter;
    private final io.micrometer.core.instrument.DistributionSummary orderAmountSummary;
    
    // 支付相关指标
    private final Counter paymentTotalCounter;
    private final Counter paymentSuccessCounter;
    private final Counter paymentFailedCounter;
    
    // 库存相关指标
    private final Counter inventoryDeductedCounter;
    private final Counter inventoryDeductFailedCounter;
    
    // 缓存相关指标
    private final AtomicLong cacheHitCount = new AtomicLong(0);
    private final AtomicLong cacheMissCount = new AtomicLong(0);
    
    // 数据库连接池指标
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 初始化订单指标
        this.orderCreatedCounter = Counter.builder("order_created_total")
                .description("订单创建总数")
                .tag("application", "order-service")
                .register(meterRegistry);
        
        this.orderAmountSummary = io.micrometer.core.instrument.DistributionSummary.builder("order_amount")
                .description("订单金额分布")
                .baseUnit("yuan")
                .publishPercentiles(0.5, 0.9, 0.99)
                .register(meterRegistry);
        
        // 初始化支付指标
        this.paymentTotalCounter = Counter.builder("payment_total")
                .description("支付总次数")
                .tag("application", "payment-service")
                .register(meterRegistry);
        
        this.paymentSuccessCounter = Counter.builder("payment_success_total")
                .description("支付成功次数")
                .tag("application", "payment-service")
                .register(meterRegistry);
        
        this.paymentFailedCounter = Counter.builder("payment_failed_total")
                .description("支付失败次数")
                .tag("application", "payment-service")
                .register(meterRegistry);
        
        // 初始化库存指标
        this.inventoryDeductedCounter = Counter.builder("inventory_deducted_total")
                .description("库存扣减成功次数")
                .tag("application", "inventory-service")
                .register(meterRegistry);
        
        this.inventoryDeductFailedCounter = Counter.builder("inventory_deduct_failed_total")
                .description("库存扣减失败次数")
                .tag("application", "inventory-service")
                .register(meterRegistry);
        
        // 初始化缓存命中率指标
        Gauge.builder("cache_hit_rate", this, BusinessMetrics::calculateCacheHitRate)
                .description("缓存命中率")
                .tag("cache_level", "local")
                .register(meterRegistry);
        
        // 初始化数据库连接池指标
        Gauge.builder("database_active_connections", activeConnections, AtomicInteger::get)
                .description("数据库活跃连接数")
                .register(meterRegistry);
    }

    /**
     * 记录订单创建
     * @param status 订单状态（SUCCESS, FAILED）
     */
    public void recordOrderCreated(String status) {
        orderCreatedCounter.increment();
        log.debug("记录订单创建指标: status={}", status);
    }

    /**
     * 记录订单金额
     * @param amount 订单金额
     */
    public void recordOrderAmount(BigDecimal amount) {
        if (amount != null) {
            orderAmountSummary.record(amount.doubleValue());
            log.debug("记录订单金额指标: amount={}", amount);
        }
    }

    /**
     * 记录支付操作
     * @param success 是否成功
     */
    public void recordPayment(boolean success) {
        paymentTotalCounter.increment();
        if (success) {
            paymentSuccessCounter.increment();
        } else {
            paymentFailedCounter.increment();
        }
        log.debug("记录支付指标: success={}", success);
    }

    /**
     * 记录库存扣减操作
     * @param success 是否成功
     */
    public void recordInventoryDeduction(boolean success) {
        if (success) {
            inventoryDeductedCounter.increment();
        } else {
            inventoryDeductFailedCounter.increment();
        }
        log.debug("记录库存扣减指标: success={}", success);
    }

    /**
     * 记录缓存命中
     * @param level 缓存级别（local, redis）
     */
    public void recordCacheHit(String level) {
        cacheHitCount.incrementAndGet();
        log.debug("记录缓存命中: level={}", level);
    }

    /**
     * 记录缓存未命中
     * @param level 缓存级别（local, redis）
     */
    public void recordCacheMiss(String level) {
        cacheMissCount.incrementAndGet();
        log.debug("记录缓存未命中: level={}", level);
    }

    /**
     * 计算缓存命中率
     * @return 缓存命中率（0-100）
     */
    private double calculateCacheHitRate() {
        long hits = cacheHitCount.get();
        long misses = cacheMissCount.get();
        long total = hits + misses;
        
        if (total == 0) {
            return 0.0;
        }
        
        return (double) hits / total * 100.0;
    }

    /**
     * 设置数据库活跃连接数
     * @param count 连接数
     */
    public void setActiveConnections(int count) {
        activeConnections.set(count);
    }

    /**
     * 创建自定义计时器
     * @param name 指标名称
     * @param description 描述
     * @return Timer 实例
     */
    public Timer createTimer(String name, String description) {
        return Timer.builder(name)
                .description(description)
                .register(meterRegistry);
    }

    /**
     * 创建自定义计数器
     * @param name 指标名称
     * @param description 描述
     * @param tags 标签
     * @return Counter 实例
     */
    public Counter createCounter(String name, String description, String... tags) {
        Counter.Builder builder = Counter.builder(name).description(description);
        
        // 添加标签
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }
        
        return builder.register(meterRegistry);
    }

    /**
     * 获取 MeterRegistry 实例
     * @return MeterRegistry
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}
