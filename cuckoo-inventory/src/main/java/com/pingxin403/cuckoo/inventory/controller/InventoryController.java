package com.pingxin403.cuckoo.inventory.controller;

import com.pingxin403.cuckoo.common.controller.BaseController;
import com.pingxin403.cuckoo.inventory.dto.InitInventoryRequest;
import com.pingxin403.cuckoo.inventory.dto.InventoryDTO;
import com.pingxin403.cuckoo.inventory.dto.InventoryOperationRequest;
import com.pingxin403.cuckoo.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 库存控制器
 * 提供库存初始化、预占、扣减、释放和查询 REST API
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController extends BaseController {

    private final InventoryService inventoryService;

    /**
     * 初始化库存
     * POST /api/inventory/init
     */
    @PostMapping("/init")
    public ResponseEntity<InventoryDTO> initInventory(@RequestBody InitInventoryRequest request) {
        logRequest("初始化库存", request.getSkuId(), request.getTotalStock());
        InventoryDTO inventory = inventoryService.initInventory(request);
        logResponse("初始化库存", inventory.getId());
        return created(inventory);
    }

    /**
     * 预占库存
     * POST /api/inventory/reserve
     */
    @PostMapping("/reserve")
    public ResponseEntity<Void> reserveStock(@RequestBody InventoryOperationRequest request) {
        logRequest("预占库存", request.getSkuId(), request.getQuantity());
        inventoryService.reserveStock(request);
        logResponse("预占库存", "成功");
        return ok(null);
    }

    /**
     * 扣减库存
     * POST /api/inventory/deduct
     */
    @PostMapping("/deduct")
    public ResponseEntity<Void> deductStock(@RequestBody InventoryOperationRequest request) {
        logRequest("扣减库存", request.getSkuId(), request.getQuantity());
        inventoryService.deductStock(request);
        logResponse("扣减库存", "成功");
        return ok(null);
    }

    /**
     * 释放库存
     * POST /api/inventory/release
     */
    @PostMapping("/release")
    public ResponseEntity<Void> releaseStock(@RequestBody InventoryOperationRequest request) {
        logRequest("释放库存", request.getSkuId(), request.getQuantity());
        inventoryService.releaseStock(request);
        logResponse("释放库存", "成功");
        return ok(null);
    }

    /**
     * 根据 SKU ID 查询库存
     * GET /api/inventory/{skuId}
     */
    @GetMapping("/{skuId}")
    public ResponseEntity<InventoryDTO> getInventory(@PathVariable Long skuId) {
        logRequest("查询库存", skuId);
        InventoryDTO inventory = inventoryService.getInventoryBySkuId(skuId);
        logResponse("查询库存", inventory.getId());
        return ok(inventory);
    }
}
