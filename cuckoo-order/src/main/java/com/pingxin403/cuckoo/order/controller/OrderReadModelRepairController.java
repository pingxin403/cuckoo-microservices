package com.pingxin403.cuckoo.order.controller;

import com.pingxin403.cuckoo.common.controller.BaseController;
import com.pingxin403.cuckoo.order.service.OrderReadModelRepairService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单读模型数据修复控制器
 * 提供手动同步和数据一致性检查接口
 */
@RestController
@RequestMapping("/api/orders/repair")
@RequiredArgsConstructor
public class OrderReadModelRepairController extends BaseController {

    private final OrderReadModelRepairService repairService;

    /**
     * 手动同步单个订单的读模型
     */
    @PostMapping("/sync/{orderId}")
    public ResponseEntity<Map<String, String>> syncSingleOrder(@PathVariable String orderId) {
        logRequest("手动同步单个订单读模型", orderId);
        repairService.syncSingleOrder(orderId);
        logResponse("手动同步单个订单读模型", "成功");
        return ok(Map.of("message", "订单读模型同步成功", "orderId", orderId));
    }

    /**
     * 批量同步所有订单的读模型
     */
    @PostMapping("/sync/all")
    public ResponseEntity<Map<String, String>> syncAllOrders() {
        logRequest("批量同步所有订单读模型");
        repairService.syncAllOrders();
        logResponse("批量同步所有订单读模型", "成功");
        return ok(Map.of("message", "所有订单读模型同步完成"));
    }

    /**
     * 检查读写模型数据一致性
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkDataConsistency() {
        logRequest("检查读写模型数据一致性");
        List<String> inconsistentOrders = repairService.checkDataConsistency();
        logResponse("检查读写模型数据一致性", inconsistentOrders.size() + " 个不一致");
        return ok(Map.of(
                "inconsistentCount", inconsistentOrders.size(),
                "inconsistentOrders", inconsistentOrders
        ));
    }

    /**
     * 修复不一致的数据
     */
    @PostMapping("/repair")
    public ResponseEntity<Map<String, String>> repairInconsistentData() {
        logRequest("修复不一致的数据");
        repairService.repairInconsistentData();
        logResponse("修复不一致的数据", "成功");
        return ok(Map.of("message", "数据修复完成"));
    }

    /**
     * 获取数据一致性报告
     */
    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getConsistencyReport() {
        logRequest("获取数据一致性报告");
        Map<String, Object> report = repairService.getConsistencyReport();
        logResponse("获取数据一致性报告", "成功");
        return ok(report);
    }
}
