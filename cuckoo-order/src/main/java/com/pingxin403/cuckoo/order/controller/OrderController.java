package com.pingxin403.cuckoo.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.pingxin403.cuckoo.common.audit.AuditLog;
import com.pingxin403.cuckoo.common.audit.Auditable;
import com.pingxin403.cuckoo.common.controller.BaseController;
import com.pingxin403.cuckoo.order.dto.CreateOrderRequest;
import com.pingxin403.cuckoo.order.dto.OrderDTO;
import com.pingxin403.cuckoo.order.service.OrderService;
import com.pingxin403.cuckoo.order.service.OrderQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "订单管理", description = "订单创建、查询、取消等操作")
public class OrderController extends BaseController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    /**
     * 创建订单
     */
    @PostMapping
    @SentinelResource(value = "POST:/api/orders")
    @Auditable(value = AuditLog.OperationType.CREATE_ORDER, resourceType = "ORDER", description = "创建订单")
    @Operation(summary = "创建订单", description = "根据用户ID、商品SKU和数量创建新订单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "订单创建成功",
                    content = @Content(schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<OrderDTO> createOrder(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "订单创建请求",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateOrderRequest.class)))
            @RequestBody CreateOrderRequest request) {
        logRequest("创建订单", request.getUserId(), request.getSkuId(), request.getQuantity());
        OrderDTO order = orderService.createOrder(request);
        logResponse("创建订单", order.getId());
        return ok(order);
    }

    /**
     * 查询订单（使用读模型）
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询订单", description = "根据订单ID查询订单详情")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    public ResponseEntity<OrderDTO> getOrder(
            @Parameter(description = "订单ID", required = true, example = "1")
            @PathVariable Long id) {
        logRequest("查询订单", id);
        OrderDTO order = orderService.getOrder(id);
        logResponse("查询订单", order.getId());
        return ok(order);
    }

    /**
     * 查询订单详情（使用 CQRS 读模型）
     */
    @GetMapping("/read/{orderId}")
    @Operation(summary = "查询订单详情（CQRS读模型）", description = "使用CQRS读模型查询订单详情，性能更优")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    public ResponseEntity<OrderDTO> getOrderByIdFromReadModel(
            @Parameter(description = "订单UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String orderId) {
        logRequest("查询订单详情（读模型）", orderId);
        OrderDTO order = orderQueryService.getOrderById(orderId);
        logResponse("查询订单详情（读模型）", orderId);
        return ok(order);
    }

    /**
     * 查询用户订单列表（使用 CQRS 读模型）
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户订单列表", description = "查询指定用户的所有订单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public ResponseEntity<List<OrderDTO>> getUserOrders(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId) {
        logRequest("查询用户订单列表（读模型）", userId);
        List<OrderDTO> orders = orderQueryService.getUserOrders(userId);
        logResponse("查询用户订单列表（读模型）", orders.size() + " 个订单");
        return ok(orders);
    }

    /**
     * 查询用户订单列表（分页，使用 CQRS 读模型）
     */
    @GetMapping("/user/{userId}/page")
    @Operation(summary = "分页查询用户订单", description = "分页查询指定用户的订单列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public ResponseEntity<?> getUserOrdersPage(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long userId,
            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小", example = "10")
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
    @Operation(summary = "根据状态查询订单", description = "查询指定状态的所有订单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(
            @Parameter(description = "订单状态", required = true, example = "PENDING")
            @PathVariable String status) {
        logRequest("根据状态查询订单列表（读模型）", status);
        List<OrderDTO> orders = orderQueryService.getOrdersByStatus(status);
        logResponse("根据状态查询订单列表（读模型）", orders.size() + " 个订单");
        return ok(orders);
    }

    /**
     * 取消订单
     */
    @PutMapping("/{id}/cancel")
    @Auditable(value = AuditLog.OperationType.CANCEL_ORDER, resourceType = "ORDER", description = "取消订单")
    @Operation(summary = "取消订单", description = "取消指定的订单")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "取消成功",
                    content = @Content(schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "404", description = "订单不存在"),
            @ApiResponse(responseCode = "400", description = "订单状态不允许取消")
    })
    public ResponseEntity<OrderDTO> cancelOrder(
            @Parameter(description = "订单ID", required = true, example = "1")
            @PathVariable Long id) {
        logRequest("取消订单", id);
        OrderDTO order = orderService.cancelOrder(id);
        logResponse("取消订单", order.getId());
        return ok(order);
    }
}
