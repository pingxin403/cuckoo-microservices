package com.pingxin403.cuckoo.inventory.service;

import com.pingxin403.cuckoo.common.exception.BusinessException;
import com.pingxin403.cuckoo.common.exception.DuplicateResourceException;
import com.pingxin403.cuckoo.common.exception.InsufficientStockException;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.inventory.config.InventoryConfig;
import com.pingxin403.cuckoo.inventory.dto.InitInventoryRequest;
import com.pingxin403.cuckoo.inventory.dto.InventoryDTO;
import com.pingxin403.cuckoo.inventory.dto.InventoryOperationRequest;
import com.pingxin403.cuckoo.inventory.entity.Inventory;
import com.pingxin403.cuckoo.inventory.entity.InventoryLog;
import com.pingxin403.cuckoo.inventory.repository.InventoryLogRepository;
import com.pingxin403.cuckoo.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/**
 * 库存服务
 * 提供库存初始化、预占、扣减、释放和查询功能。
 * 使用 Redis 分布式锁保证并发安全。
 * 库存不变量：total_stock = available_stock + reserved_stock
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final InventoryConfig inventoryConfig;

    private static final String LOCK_KEY_PREFIX = "inventory:lock:";

    /**
     * 初始化库存
     */
    @Transactional
    public InventoryDTO initInventory(InitInventoryRequest request) {
        if (inventoryRepository.existsBySkuId(request.getSkuId())) {
            throw new DuplicateResourceException(
                    "Inventory already exists for SKU: " + request.getSkuId());
        }

        Inventory inventory = Inventory.builder()
                .skuId(request.getSkuId())
                .totalStock(request.getTotalStock())
                .availableStock(request.getTotalStock())
                .reservedStock(0)
                .build();

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Inventory initialized: skuId={}, totalStock={}", saved.getSkuId(), saved.getTotalStock());

        return toDTO(saved);
    }

    /**
     * 预占库存
     * 使用 Redis 分布式锁保证并发安全
     * 操作：available_stock -= quantity, reserved_stock += quantity
     */
    public void reserveStock(InventoryOperationRequest request) {
        String lockKey = LOCK_KEY_PREFIX + request.getSkuId();
        String lockValue = UUID.randomUUID().toString();

        boolean locked = tryLock(lockKey, lockValue);
        if (!locked) {
            throw new BusinessException("服务繁忙，请稍后重试");
        }

        try {
            doReserveStock(request);
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    @Transactional
    protected void doReserveStock(InventoryOperationRequest request) {
        Inventory inventory = inventoryRepository.findBySkuId(request.getSkuId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", request.getSkuId()));

        if (inventory.getAvailableStock() < request.getQuantity()) {
            throw new InsufficientStockException(
                    request.getSkuId(), request.getQuantity(), inventory.getAvailableStock());
        }

        inventory.setAvailableStock(inventory.getAvailableStock() - request.getQuantity());
        inventory.setReservedStock(inventory.getReservedStock() + request.getQuantity());
        inventoryRepository.save(inventory);

        // 记录操作流水
        saveLog(request.getSkuId(), request.getOrderId(), "RESERVE", request.getQuantity());

        log.info("Stock reserved: skuId={}, quantity={}, orderId={}",
                request.getSkuId(), request.getQuantity(), request.getOrderId());
    }

    /**
     * 扣减库存
     * 将预占库存转为已扣减：reserved_stock -= quantity, total_stock -= quantity
     */
    public void deductStock(InventoryOperationRequest request) {
        String lockKey = LOCK_KEY_PREFIX + request.getSkuId();
        String lockValue = UUID.randomUUID().toString();

        boolean locked = tryLock(lockKey, lockValue);
        if (!locked) {
            throw new BusinessException("服务繁忙，请稍后重试");
        }

        try {
            doDeductStock(request);
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    @Transactional
    protected void doDeductStock(InventoryOperationRequest request) {
        Inventory inventory = inventoryRepository.findBySkuId(request.getSkuId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", request.getSkuId()));

        if (inventory.getReservedStock() < request.getQuantity()) {
            throw new BusinessException(String.format(
                    "Insufficient reserved stock for SKU %d: requested %d, reserved %d",
                    request.getSkuId(), request.getQuantity(), inventory.getReservedStock()));
        }

        inventory.setReservedStock(inventory.getReservedStock() - request.getQuantity());
        inventory.setTotalStock(inventory.getTotalStock() - request.getQuantity());
        inventoryRepository.save(inventory);

        // 记录操作流水
        saveLog(request.getSkuId(), request.getOrderId(), "DEDUCT", request.getQuantity());

        log.info("Stock deducted: skuId={}, quantity={}, orderId={}",
                request.getSkuId(), request.getQuantity(), request.getOrderId());
    }

    /**
     * 释放库存
     * 释放预占库存：reserved_stock -= quantity, available_stock += quantity
     */
    public void releaseStock(InventoryOperationRequest request) {
        String lockKey = LOCK_KEY_PREFIX + request.getSkuId();
        String lockValue = UUID.randomUUID().toString();

        boolean locked = tryLock(lockKey, lockValue);
        if (!locked) {
            throw new BusinessException("服务繁忙，请稍后重试");
        }

        try {
            doReleaseStock(request);
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    @Transactional
    protected void doReleaseStock(InventoryOperationRequest request) {
        Inventory inventory = inventoryRepository.findBySkuId(request.getSkuId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", request.getSkuId()));

        if (inventory.getReservedStock() < request.getQuantity()) {
            throw new BusinessException(String.format(
                    "Insufficient reserved stock to release for SKU %d: requested %d, reserved %d",
                    request.getSkuId(), request.getQuantity(), inventory.getReservedStock()));
        }

        inventory.setReservedStock(inventory.getReservedStock() - request.getQuantity());
        inventory.setAvailableStock(inventory.getAvailableStock() + request.getQuantity());
        inventoryRepository.save(inventory);

        // 记录操作流水
        saveLog(request.getSkuId(), request.getOrderId(), "RELEASE", request.getQuantity());

        log.info("Stock released: skuId={}, quantity={}, orderId={}",
                request.getSkuId(), request.getQuantity(), request.getOrderId());
    }

    /**
     * 根据 SKU ID 查询库存
     */
    @Transactional(readOnly = true)
    public InventoryDTO getInventoryBySkuId(Long skuId) {
        Inventory inventory = inventoryRepository.findBySkuId(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", skuId));
        return toDTO(inventory);
    }

    /**
     * 尝试获取 Redis 分布式锁
     */
    private boolean tryLock(String lockKey, String lockValue) {
        int timeout = inventoryConfig.getLockTimeout();
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(timeout));
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放 Redis 分布式锁（仅释放自己持有的锁）
     */
    private void unlock(String lockKey, String lockValue) {
        String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(currentValue)) {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 记录库存操作流水日志
     */
    private void saveLog(Long skuId, String orderId, String operation, Integer quantity) {
        InventoryLog logEntry = InventoryLog.builder()
                .skuId(skuId)
                .orderId(orderId)
                .operation(operation)
                .quantity(quantity)
                .build();
        inventoryLogRepository.save(logEntry);
    }

    /**
     * 将 Inventory 实体转换为 InventoryDTO
     */
    private InventoryDTO toDTO(Inventory inventory) {
        return InventoryDTO.builder()
                .id(inventory.getId())
                .skuId(inventory.getSkuId())
                .totalStock(inventory.getTotalStock())
                .availableStock(inventory.getAvailableStock())
                .reservedStock(inventory.getReservedStock())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
