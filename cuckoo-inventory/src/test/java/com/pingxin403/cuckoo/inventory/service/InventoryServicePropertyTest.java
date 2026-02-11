package com.pingxin403.cuckoo.inventory.service;

import com.pingxin403.cuckoo.common.exception.InsufficientStockException;
import com.pingxin403.cuckoo.inventory.config.TestConfig;
import com.pingxin403.cuckoo.inventory.dto.InitInventoryRequest;
import com.pingxin403.cuckoo.inventory.dto.InventoryDTO;
import com.pingxin403.cuckoo.inventory.dto.InventoryOperationRequest;
import com.pingxin403.cuckoo.inventory.entity.Inventory;
import com.pingxin403.cuckoo.inventory.repository.InventoryLogRepository;
import com.pingxin403.cuckoo.inventory.repository.InventoryRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Positive;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for InventoryService
 * 
 * Tests core invariants:
 * - Inventory invariant: total_stock = available_stock + reserved_stock
 * - Stock operations maintain consistency
 * - Reserve/deduct/release operations are correct
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class InventoryServicePropertyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    /**
     * Property: Inventory initialization creates valid inventory with correct stock values
     * Validates: Requirements 2.1 - Inventory management
     */
    @Property(tries = 100)
    @Transactional
    void initInventory_createsValidInventory(
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 1, max = 10000) int totalStock) {
        
        // Clean up any existing inventory
        inventoryRepository.deleteAll();
        
        InitInventoryRequest request = new InitInventoryRequest();
        request.setSkuId(skuId);
        request.setTotalStock(totalStock);

        InventoryDTO result = inventoryService.initInventory(request);

        assertThat(result).isNotNull();
        assertThat(result.getSkuId()).isEqualTo(skuId);
        assertThat(result.getTotalStock()).isEqualTo(totalStock);
        assertThat(result.getAvailableStock()).isEqualTo(totalStock);
        assertThat(result.getReservedStock()).isEqualTo(0);
        
        // Verify invariant: total = available + reserved
        assertThat(result.getTotalStock())
                .isEqualTo(result.getAvailableStock() + result.getReservedStock());
    }

    /**
     * Property: Stock reservation maintains inventory invariant
     * Validates: Requirements 2.1 - Stock reservation logic
     */
    @Property(tries = 100)
    @Transactional
    void reserveStock_maintainsInventoryInvariant(
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 100, max = 1000) int totalStock,
            @ForAll @IntRange(min = 1, max = 50) int reserveQuantity) {
        
        // Setup: Initialize inventory
        inventoryRepository.deleteAll();
        Inventory inventory = Inventory.builder()
                .skuId(skuId)
                .totalStock(totalStock)
                .availableStock(totalStock)
                .reservedStock(0)
                .build();
        inventoryRepository.save(inventory);

        // Execute: Reserve stock
        InventoryOperationRequest request = new InventoryOperationRequest();
        request.setSkuId(skuId);
        request.setOrderId("ORDER-" + skuId);
        request.setQuantity(reserveQuantity);

        inventoryService.reserveStock(request);

        // Verify: Invariant maintained
        Inventory updated = inventoryRepository.findBySkuId(skuId).orElseThrow();
        assertThat(updated.getTotalStock())
                .isEqualTo(updated.getAvailableStock() + updated.getReservedStock());
        assertThat(updated.getTotalStock()).isEqualTo(totalStock);
        assertThat(updated.getAvailableStock()).isEqualTo(totalStock - reserveQuantity);
        assertThat(updated.getReservedStock()).isEqualTo(reserveQuantity);
    }

    /**
     * Property: Stock deduction maintains inventory invariant
     * Validates: Requirements 2.1 - Stock deduction logic
     */
    @Property(tries = 100)
    @Transactional
    void deductStock_maintainsInventoryInvariant(
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 100, max = 1000) int totalStock,
            @ForAll @IntRange(min = 1, max = 50) int quantity) {
        
        // Setup: Initialize inventory with reserved stock
        inventoryRepository.deleteAll();
        Inventory inventory = Inventory.builder()
                .skuId(skuId)
                .totalStock(totalStock)
                .availableStock(totalStock - quantity)
                .reservedStock(quantity)
                .build();
        inventoryRepository.save(inventory);

        // Execute: Deduct stock
        InventoryOperationRequest request = new InventoryOperationRequest();
        request.setSkuId(skuId);
        request.setOrderId("ORDER-" + skuId);
        request.setQuantity(quantity);

        inventoryService.deductStock(request);

        // Verify: Invariant maintained
        Inventory updated = inventoryRepository.findBySkuId(skuId).orElseThrow();
        assertThat(updated.getTotalStock())
                .isEqualTo(updated.getAvailableStock() + updated.getReservedStock());
        assertThat(updated.getTotalStock()).isEqualTo(totalStock - quantity);
        assertThat(updated.getReservedStock()).isEqualTo(0);
    }

    /**
     * Property: Stock release maintains inventory invariant
     * Validates: Requirements 2.1 - Stock release logic
     */
    @Property(tries = 100)
    @Transactional
    void releaseStock_maintainsInventoryInvariant(
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 100, max = 1000) int totalStock,
            @ForAll @IntRange(min = 1, max = 50) int quantity) {
        
        // Setup: Initialize inventory with reserved stock
        inventoryRepository.deleteAll();
        Inventory inventory = Inventory.builder()
                .skuId(skuId)
                .totalStock(totalStock)
                .availableStock(totalStock - quantity)
                .reservedStock(quantity)
                .build();
        inventoryRepository.save(inventory);

        // Execute: Release stock
        InventoryOperationRequest request = new InventoryOperationRequest();
        request.setSkuId(skuId);
        request.setOrderId("ORDER-" + skuId);
        request.setQuantity(quantity);

        inventoryService.releaseStock(request);

        // Verify: Invariant maintained
        Inventory updated = inventoryRepository.findBySkuId(skuId).orElseThrow();
        assertThat(updated.getTotalStock())
                .isEqualTo(updated.getAvailableStock() + updated.getReservedStock());
        assertThat(updated.getTotalStock()).isEqualTo(totalStock);
        assertThat(updated.getAvailableStock()).isEqualTo(totalStock);
        assertThat(updated.getReservedStock()).isEqualTo(0);
    }

    /**
     * Property: Reserve-then-deduct sequence maintains consistency
     * Validates: Requirements 2.1 - Complete stock operation flow
     */
    @Property(tries = 100)
    @Transactional
    void reserveThenDeduct_maintainsConsistency(
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 100, max = 1000) int totalStock,
            @ForAll @IntRange(min = 1, max = 50) int quantity) {
        
        // Setup
        inventoryRepository.deleteAll();
        Inventory inventory = Inventory.builder()
                .skuId(skuId)
                .totalStock(totalStock)
                .availableStock(totalStock)
                .reservedStock(0)
                .build();
        inventoryRepository.save(inventory);

        String orderId = "ORDER-" + skuId;

        // Execute: Reserve
        InventoryOperationRequest reserveRequest = new InventoryOperationRequest();
        reserveRequest.setSkuId(skuId);
        reserveRequest.setOrderId(orderId);
        reserveRequest.setQuantity(quantity);
        inventoryService.reserveStock(reserveRequest);

        // Execute: Deduct
        InventoryOperationRequest deductRequest = new InventoryOperationRequest();
        deductRequest.setSkuId(skuId);
        deductRequest.setOrderId(orderId);
        deductRequest.setQuantity(quantity);
        inventoryService.deductStock(deductRequest);

        // Verify: Final state is consistent
        Inventory updated = inventoryRepository.findBySkuId(skuId).orElseThrow();
        assertThat(updated.getTotalStock()).isEqualTo(totalStock - quantity);
        assertThat(updated.getAvailableStock()).isEqualTo(totalStock - quantity);
        assertThat(updated.getReservedStock()).isEqualTo(0);
        assertThat(updated.getTotalStock())
                .isEqualTo(updated.getAvailableStock() + updated.getReservedStock());
    }

    /**
     * Property: Reserve-then-release sequence restores original state
     * Validates: Requirements 2.1 - Stock cancellation flow
     */
    @Property(tries = 100)
    @Transactional
    void reserveThenRelease_restoresOriginalState(
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 100, max = 1000) int totalStock,
            @ForAll @IntRange(min = 1, max = 50) int quantity) {
        
        // Setup
        inventoryRepository.deleteAll();
        Inventory inventory = Inventory.builder()
                .skuId(skuId)
                .totalStock(totalStock)
                .availableStock(totalStock)
                .reservedStock(0)
                .build();
        inventoryRepository.save(inventory);

        String orderId = "ORDER-" + skuId;

        // Execute: Reserve
        InventoryOperationRequest reserveRequest = new InventoryOperationRequest();
        reserveRequest.setSkuId(skuId);
        reserveRequest.setOrderId(orderId);
        reserveRequest.setQuantity(quantity);
        inventoryService.reserveStock(reserveRequest);

        // Execute: Release
        InventoryOperationRequest releaseRequest = new InventoryOperationRequest();
        releaseRequest.setSkuId(skuId);
        releaseRequest.setOrderId(orderId);
        releaseRequest.setQuantity(quantity);
        inventoryService.releaseStock(releaseRequest);

        // Verify: State restored to original
        Inventory updated = inventoryRepository.findBySkuId(skuId).orElseThrow();
        assertThat(updated.getTotalStock()).isEqualTo(totalStock);
        assertThat(updated.getAvailableStock()).isEqualTo(totalStock);
        assertThat(updated.getReservedStock()).isEqualTo(0);
    }

    /**
     * Property: Cannot reserve more than available stock
     * Validates: Requirements 2.1 - Stock validation
     */
    @Property(tries = 100)
    @Transactional
    void reserveStock_failsWhenInsufficientStock(
            @ForAll @Positive Long skuId,
            @ForAll @IntRange(min = 10, max = 100) int totalStock,
            @ForAll @IntRange(min = 101, max = 200) int excessiveQuantity) {
        
        // Setup
        inventoryRepository.deleteAll();
        Inventory inventory = Inventory.builder()
                .skuId(skuId)
                .totalStock(totalStock)
                .availableStock(totalStock)
                .reservedStock(0)
                .build();
        inventoryRepository.save(inventory);

        // Execute & Verify: Should throw exception
        InventoryOperationRequest request = new InventoryOperationRequest();
        request.setSkuId(skuId);
        request.setOrderId("ORDER-" + skuId);
        request.setQuantity(excessiveQuantity);

        try {
            inventoryService.reserveStock(request);
            throw new AssertionError("Expected InsufficientStockException");
        } catch (InsufficientStockException e) {
            // Expected - verify inventory unchanged
            Inventory unchanged = inventoryRepository.findBySkuId(skuId).orElseThrow();
            assertThat(unchanged.getTotalStock()).isEqualTo(totalStock);
            assertThat(unchanged.getAvailableStock()).isEqualTo(totalStock);
            assertThat(unchanged.getReservedStock()).isEqualTo(0);
        }
    }
}
