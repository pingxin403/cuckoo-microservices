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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InventoryService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryLogRepository inventoryLogRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private InventoryConfig inventoryConfig;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        testInventory = Inventory.builder()
                .id(1L)
                .skuId(100L)
                .totalStock(100)
                .availableStock(80)
                .reservedStock(20)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========== InitInventory Tests ==========

    @Test
    @DisplayName("initInventory - should create inventory and return InventoryDTO")
    void initInventory_success() {
        InitInventoryRequest request = new InitInventoryRequest(100L, 50);

        when(inventoryRepository.existsBySkuId(100L)).thenReturn(false);

        Inventory savedInventory = Inventory.builder()
                .id(1L)
                .skuId(100L)
                .totalStock(50)
                .availableStock(50)
                .reservedStock(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(savedInventory);

        InventoryDTO result = inventoryService.initInventory(request);

        assertThat(result).isNotNull();
        assertThat(result.getSkuId()).isEqualTo(100L);
        assertThat(result.getTotalStock()).isEqualTo(50);
        assertThat(result.getAvailableStock()).isEqualTo(50);
        assertThat(result.getReservedStock()).isEqualTo(0);

        verify(inventoryRepository).save(any(Inventory.class));
    }

    @Test
    @DisplayName("initInventory - should throw DuplicateResourceException when SKU already exists")
    void initInventory_duplicate() {
        InitInventoryRequest request = new InitInventoryRequest(100L, 50);

        when(inventoryRepository.existsBySkuId(100L)).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.initInventory(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU");
    }

    // ========== ReserveStock Tests ==========

    @Test
    @DisplayName("reserveStock - should reserve stock successfully with distributed lock")
    void reserveStock_success() {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 10, "order-001");

        // Mock Redis lock
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(inventoryConfig.getLockTimeout()).thenReturn(5);

        // Mock inventory lookup
        Inventory inventory = Inventory.builder()
                .id(1L).skuId(100L).totalStock(100).availableStock(80).reservedStock(20)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(inventoryRepository.findBySkuId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenReturn(new InventoryLog());

        // Mock unlock
        when(valueOperations.get(anyString())).thenReturn(null);

        inventoryService.reserveStock(request);

        // Verify inventory was updated
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        Inventory saved = inventoryCaptor.getValue();
        assertThat(saved.getAvailableStock()).isEqualTo(70);
        assertThat(saved.getReservedStock()).isEqualTo(30);

        // Verify log was created
        ArgumentCaptor<InventoryLog> logCaptor = ArgumentCaptor.forClass(InventoryLog.class);
        verify(inventoryLogRepository).save(logCaptor.capture());
        InventoryLog logEntry = logCaptor.getValue();
        assertThat(logEntry.getOperation()).isEqualTo("RESERVE");
        assertThat(logEntry.getQuantity()).isEqualTo(10);
        assertThat(logEntry.getOrderId()).isEqualTo("order-001");
    }

    @Test
    @DisplayName("reserveStock - should throw InsufficientStockException when stock is not enough")
    void reserveStock_insufficientStock() {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 100, "order-002");

        // Mock Redis lock
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(inventoryConfig.getLockTimeout()).thenReturn(5);

        // Mock inventory with only 10 available
        Inventory inventory = Inventory.builder()
                .id(1L).skuId(100L).totalStock(100).availableStock(10).reservedStock(90)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(inventoryRepository.findBySkuId(100L)).thenReturn(Optional.of(inventory));

        // Mock unlock
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> inventoryService.reserveStock(request))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("reserveStock - should throw BusinessException when lock acquisition fails")
    void reserveStock_lockFailed() {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 10, "order-003");

        // Mock Redis lock failure
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(inventoryConfig.getLockTimeout()).thenReturn(5);

        assertThatThrownBy(() -> inventoryService.reserveStock(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("服务繁忙");
    }

    // ========== DeductStock Tests ==========

    @Test
    @DisplayName("deductStock - should deduct reserved stock successfully")
    void deductStock_success() {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 10, "order-001");

        // Mock Redis lock
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(inventoryConfig.getLockTimeout()).thenReturn(5);

        // Mock inventory
        Inventory inventory = Inventory.builder()
                .id(1L).skuId(100L).totalStock(100).availableStock(70).reservedStock(30)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(inventoryRepository.findBySkuId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenReturn(new InventoryLog());

        // Mock unlock
        when(valueOperations.get(anyString())).thenReturn(null);

        inventoryService.deductStock(request);

        // Verify: total_stock -= quantity, reserved_stock -= quantity
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        Inventory saved = inventoryCaptor.getValue();
        assertThat(saved.getTotalStock()).isEqualTo(90);
        assertThat(saved.getReservedStock()).isEqualTo(20);
        assertThat(saved.getAvailableStock()).isEqualTo(70); // unchanged

        // Verify log
        ArgumentCaptor<InventoryLog> logCaptor = ArgumentCaptor.forClass(InventoryLog.class);
        verify(inventoryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getOperation()).isEqualTo("DEDUCT");
    }

    @Test
    @DisplayName("deductStock - should throw BusinessException when reserved stock is insufficient")
    void deductStock_insufficientReserved() {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 50, "order-004");

        // Mock Redis lock
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(inventoryConfig.getLockTimeout()).thenReturn(5);

        // Mock inventory with only 10 reserved
        Inventory inventory = Inventory.builder()
                .id(1L).skuId(100L).totalStock(100).availableStock(90).reservedStock(10)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(inventoryRepository.findBySkuId(100L)).thenReturn(Optional.of(inventory));

        // Mock unlock
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> inventoryService.deductStock(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient reserved stock");
    }

    // ========== ReleaseStock Tests ==========

    @Test
    @DisplayName("releaseStock - should release reserved stock successfully")
    void releaseStock_success() {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 10, "order-001");

        // Mock Redis lock
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(inventoryConfig.getLockTimeout()).thenReturn(5);

        // Mock inventory
        Inventory inventory = Inventory.builder()
                .id(1L).skuId(100L).totalStock(100).availableStock(70).reservedStock(30)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(inventoryRepository.findBySkuId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenReturn(new InventoryLog());

        // Mock unlock
        when(valueOperations.get(anyString())).thenReturn(null);

        inventoryService.releaseStock(request);

        // Verify: available_stock += quantity, reserved_stock -= quantity
        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        Inventory saved = inventoryCaptor.getValue();
        assertThat(saved.getAvailableStock()).isEqualTo(80);
        assertThat(saved.getReservedStock()).isEqualTo(20);
        assertThat(saved.getTotalStock()).isEqualTo(100); // unchanged

        // Verify log
        ArgumentCaptor<InventoryLog> logCaptor = ArgumentCaptor.forClass(InventoryLog.class);
        verify(inventoryLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getOperation()).isEqualTo("RELEASE");
    }

    @Test
    @DisplayName("releaseStock - should throw BusinessException when reserved stock is insufficient to release")
    void releaseStock_insufficientReserved() {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 50, "order-005");

        // Mock Redis lock
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(inventoryConfig.getLockTimeout()).thenReturn(5);

        // Mock inventory with only 10 reserved
        Inventory inventory = Inventory.builder()
                .id(1L).skuId(100L).totalStock(100).availableStock(90).reservedStock(10)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(inventoryRepository.findBySkuId(100L)).thenReturn(Optional.of(inventory));

        // Mock unlock
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> inventoryService.releaseStock(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient reserved stock to release");
    }

    // ========== GetInventoryBySkuId Tests ==========

    @Test
    @DisplayName("getInventoryBySkuId - should return InventoryDTO when inventory exists")
    void getInventoryBySkuId_success() {
        when(inventoryRepository.findBySkuId(100L)).thenReturn(Optional.of(testInventory));

        InventoryDTO result = inventoryService.getInventoryBySkuId(100L);

        assertThat(result).isNotNull();
        assertThat(result.getSkuId()).isEqualTo(100L);
        assertThat(result.getTotalStock()).isEqualTo(100);
        assertThat(result.getAvailableStock()).isEqualTo(80);
        assertThat(result.getReservedStock()).isEqualTo(20);
    }

    @Test
    @DisplayName("getInventoryBySkuId - should throw ResourceNotFoundException when inventory not found")
    void getInventoryBySkuId_notFound() {
        when(inventoryRepository.findBySkuId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventoryBySkuId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inventory");
    }

    // ========== Invariant Tests ==========

    @Test
    @DisplayName("reserve operation should maintain invariant: total_stock = available_stock + reserved_stock")
    void reserveStock_maintainsInvariant() {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 15, "order-inv");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(inventoryConfig.getLockTimeout()).thenReturn(5);

        Inventory inventory = Inventory.builder()
                .id(1L).skuId(100L).totalStock(100).availableStock(100).reservedStock(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(inventoryRepository.findBySkuId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenReturn(new InventoryLog());
        when(valueOperations.get(anyString())).thenReturn(null);

        inventoryService.reserveStock(request);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        Inventory saved = captor.getValue();

        // Invariant check: total_stock = available_stock + reserved_stock
        assertThat(saved.getTotalStock())
                .isEqualTo(saved.getAvailableStock() + saved.getReservedStock());
    }

    @Test
    @DisplayName("deduct operation should maintain invariant: total_stock = available_stock + reserved_stock")
    void deductStock_maintainsInvariant() {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 10, "order-inv2");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(inventoryConfig.getLockTimeout()).thenReturn(5);

        Inventory inventory = Inventory.builder()
                .id(1L).skuId(100L).totalStock(100).availableStock(80).reservedStock(20)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(inventoryRepository.findBySkuId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenReturn(new InventoryLog());
        when(valueOperations.get(anyString())).thenReturn(null);

        inventoryService.deductStock(request);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        Inventory saved = captor.getValue();

        // Invariant check: total_stock = available_stock + reserved_stock
        assertThat(saved.getTotalStock())
                .isEqualTo(saved.getAvailableStock() + saved.getReservedStock());
    }

    @Test
    @DisplayName("release operation should maintain invariant: total_stock = available_stock + reserved_stock")
    void releaseStock_maintainsInvariant() {
        InventoryOperationRequest request = new InventoryOperationRequest(100L, 10, "order-inv3");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(inventoryConfig.getLockTimeout()).thenReturn(5);

        Inventory inventory = Inventory.builder()
                .id(1L).skuId(100L).totalStock(100).availableStock(80).reservedStock(20)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(inventoryRepository.findBySkuId(100L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenReturn(new InventoryLog());
        when(valueOperations.get(anyString())).thenReturn(null);

        inventoryService.releaseStock(request);

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(captor.capture());
        Inventory saved = captor.getValue();

        // Invariant check: total_stock = available_stock + reserved_stock
        assertThat(saved.getTotalStock())
                .isEqualTo(saved.getAvailableStock() + saved.getReservedStock());
    }
}
