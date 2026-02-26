package com.pingxin403.cuckoo.common.controller;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Base controller providing unified response patterns and logging.
 * All service controllers should extend this class to reduce boilerplate.
 * 
 * <p>Features:
 * <ul>
 *   <li>Unified HTTP response methods (created, ok, noContent)</li>
 *   <li>Request/response logging with traceId from MDC</li>
 *   <li>Generic type support for DTOs</li>
 * </ul>
 * 
 * <p>Usage Example:
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/products")
 * @RequiredArgsConstructor
 * public class ProductController extends BaseController {
 *     private final ProductService productService;
 *     
 *     @PostMapping
 *     public ResponseEntity<ProductDTO> createProduct(@RequestBody CreateProductRequest request) {
 *         logRequest("创建商品", request.getName(), request.getPrice());
 *         ProductDTO product = productService.createProduct(request);
 *         logResponse("创建商品", product.getId());
 *         return created(product);  // Returns 201 Created
 *     }
 *     
 *     @GetMapping("/{id}")
 *     public ResponseEntity<ProductDTO> getProduct(@PathVariable Long id) {
 *         logRequest("查询商品", id);
 *         ProductDTO product = productService.getProductById(id);
 *         logResponse("查询商品", product.getId());
 *         return ok(product);  // Returns 200 OK
 *     }
 *     
 *     @DeleteMapping("/{id}")
 *     public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
 *         logRequest("删除商品", id);
 *         productService.deleteProduct(id);
 *         logResponse("删除商品", id);
 *         return noContent();  // Returns 204 No Content
 *     }
 * }
 * }</pre>
 * 
 * @author cuckoo-team
 */
@Slf4j
public abstract class BaseController {

    /**
     * Returns HTTP 201 Created response with body.
     * 
     * <p>Use this method when creating a new resource successfully.
     * 
     * @param body Response body containing the created resource
     * @param <T> Response type
     * @return ResponseEntity with 201 status and the provided body
     */
    protected <T> ResponseEntity<T> created(T body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * Returns HTTP 200 OK response with body.
     * 
     * <p>Use this method for successful GET, PUT, or PATCH operations.
     * 
     * @param body Response body
     * @param <T> Response type
     * @return ResponseEntity with 200 status and the provided body
     */
    protected <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok(body);
    }

    /**
     * Returns HTTP 204 No Content response.
     * 
     * <p>Use this method for successful DELETE operations or when no response body is needed.
     * 
     * @return ResponseEntity with 204 status and no body
     */
    protected ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Logs incoming request with operation name, parameters, and traceId from MDC.
     * 
     * <p>The traceId is automatically retrieved from the MDC (Mapped Diagnostic Context)
     * which is populated by the distributed tracing system (OpenTelemetry/Jaeger).
     * 
     * <p>Log format:
     * <ul>
     *   <li>With params: {@code [traceId] 请求: operation, 参数: [param1, param2, ...]}</li>
     *   <li>Without params: {@code [traceId] 请求: operation}</li>
     * </ul>
     * 
     * @param operation Operation name (e.g., "创建商品", "查询订单")
     * @param params Request parameters (varargs, can be empty)
     */
    protected void logRequest(String operation, Object... params) {
        String traceId = MDC.get("traceId");
        if (params.length == 0) {
            log.info("[{}] 请求: {}", traceId, operation);
        } else {
            log.info("[{}] 请求: {}, 参数: {}", traceId, operation, params);
        }
    }

    /**
     * Logs outgoing response with operation name, result, and traceId from MDC.
     * 
     * <p>The traceId is automatically retrieved from the MDC (Mapped Diagnostic Context)
     * which is populated by the distributed tracing system (OpenTelemetry/Jaeger).
     * 
     * <p>Log format: {@code [traceId] 响应: operation, 结果: result}
     * 
     * @param operation Operation name (e.g., "创建商品", "查询订单")
     * @param result Response result (typically an ID or summary of the operation)
     */
    protected void logResponse(String operation, Object result) {
        String traceId = MDC.get("traceId");
        log.info("[{}] 响应: {}, 结果: {}", traceId, operation, result);
    }
}
