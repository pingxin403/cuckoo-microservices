package com.pingxin403.cuckoo.inventory.controller;

import com.pingxin403.cuckoo.inventory.dto.InitInventoryRequest;
import com.pingxin403.cuckoo.inventory.dto.InventoryDTO;
import com.pingxin403.cuckoo.inventory.dto.InventoryOperationRequest;
import com.pingxin403.cuckoo.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 库存控制器
 * 提供库存初始化、预占、扣减、释放和查询 REST API
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 初始化库存
     * POST /api/inventory/init
     */
    @PostMapping("/init")
    public ResponseEntity<InventoryDTO> initInventory(@RequestBody InitInventoryRequest request) {
        InventoryDTO inventory = inventoryService.initInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(inventory);
    }

    /**
     * 预占库存
     * POST /api/inventory/reserve
     */
    @PostMapping("/reserve")
    public ResponseEntity<Void> reserveStock(@RequestBody InventoryOperationRequest request) {
        inventoryService.reserveStock(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 扣减库存
     * POST /api/inventory/deduct
     */
    @PostMapping("/deduct")
    public ResponseEntity<Void> deductStock(@RequestBody InventoryOperationRequest request) {
        inventoryService.deductStock(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 释放库存
     * POST /api/inventory/release
     */
    @PostMapping("/release")
    public ResponseEntity<Void> releaseStock(@RequestBody InventoryOperationRequest request) {
        inventoryService.releaseStock(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 根据 SKU ID 查询库存
     * GET /api/inventory/{skuId}
     */
    @GetMapping("/{skuId}")
    public ResponseEntity<InventoryDTO> getInventory(@PathVariable Long skuId) {
        InventoryDTO inventory = inventoryService.getInventoryBySkuId(skuId);
        return ResponseEntity.ok(inventory);
    }
}
