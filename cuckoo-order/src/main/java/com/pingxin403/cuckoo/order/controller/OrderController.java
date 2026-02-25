package com.pingxin403.cuckoo.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.pingxin403.cuckoo.common.controller.BaseController;
import com.pingxin403.cuckoo.order.dto.CreateOrderRequest;
import com.pingxin403.cuckoo.order.dto.OrderDTO;
import com.pingxin403.cuckoo.order.service.OrderService;
import com.pingxin403.cuckoo.order.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController extends BaseController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    /**
     * 创建订单
     */
    @PostMapping
    @SentinelResource(value = "POST:/api/orders")
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        logRequest("创建订单", request.getUserId(), request.getSkuId(), request.getQuantity());
        OrderDTO order = orderService.createOrder(request);
        logResponse("创建订单", order.getId());
        return ok(order);
    }

    /**
     * 查询订单（使用读模型）
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        logRequest("查询订单", id);
        OrderDTO order = orderService.getOrder(id);
        logResponse("查询订单", order.getId());
        return ok(order);
    }

    /**
     * 查询订单详情（使用 CQRS 读模型）
     */
    @GetMapping("/read/{orderId}")
    public ResponseEntity<OrderDTO> getOrderByIdFromReadModel(@PathVariable String orderId) {
        logRequest("查询订单详情（读模型）", orderId);
        OrderDTO order = orderQueryService.getOrderById(orderId);
        logResponse("查询订单详情（读模型）", orderId);
        return ok(order);
    }

    /**
     * 查询用户订单列表（使用 CQRS 读模型）
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getUserOrders(@PathVariable Long userId) {
        logRequest("查询用户订单列表（读模型）", userId);
        List<OrderDTO> orders = orderQueryService.getUserOrders(userId);
        logResponse("查询用户订单列表（读模型）", orders.size() + " 个订单");
        return ok(orders);
    }

    /**
     * 查询用户订单列表（分页，使用 CQRS 读模型）
     */
    @GetMapping("/user/{userId}/page")
    public ResponseEntity<?> getUserOrdersPage(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logRequest("查询用户订单列表（分页，读模型）", userId, page, size);
        var ordersPage = orderQueryService.getUserOrdersPage(userId, page, size);
        logResponse("查询用户订单列表（分页，读模型）", ordersPage.getTotalElements() + " 个订单");
        return ok(ordersPage);
    }

    /**
     * 根据状态查询订单列表（使用 CQRS 读模型）
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(@PathVariable String status) {
        logRequest("根据状态查询订单列表（读模型）", status);
        List<OrderDTO> orders = orderQueryService.getOrdersByStatus(status);
        logResponse("根据状态查询订单列表（读模型）", orders.size() + " 个订单");
        return ok(orders);
    }

    /**
     * 取消订单
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id) {
        logRequest("取消订单", id);
        OrderDTO order = orderService.cancelOrder(id);
        logResponse("取消订单", order.getId());
        return ok(order);
    }
}
