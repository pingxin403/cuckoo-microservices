package com.pingxin403.cuckoo.order.controller;

import com.pingxin403.cuckoo.common.controller.BaseController;
import com.pingxin403.cuckoo.order.dto.CreateOrderRequest;
import com.pingxin403.cuckoo.order.dto.OrderDTO;
import com.pingxin403.cuckoo.order.service.OrderService;
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

    /**
     * 创建订单
     */
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        logRequest("创建订单", request.getUserId(), request.getSkuId(), request.getQuantity());
        OrderDTO order = orderService.createOrder(request);
        logResponse("创建订单", order.getId());
        return ok(order);
    }

    /**
     * 查询订单
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        logRequest("查询订单", id);
        OrderDTO order = orderService.getOrder(id);
        logResponse("查询订单", order.getId());
        return ok(order);
    }

    /**
     * 查询用户订单列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDTO>> getUserOrders(@PathVariable Long userId) {
        logRequest("查询用户订单列表", userId);
        List<OrderDTO> orders = orderService.getUserOrders(userId);
        logResponse("查询用户订单列表", orders.size() + " 个订单");
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
