package com.pingxin403.cuckoo.common.feign;

import com.pingxin403.cuckoo.common.exception.BusinessException;
import com.pingxin403.cuckoo.common.exception.SystemException;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Base Feign configuration providing unified error handling and tracing.
 * All services should use this configuration for Feign clients.
 * 
 * <p>Features:
 * <ul>
 *   <li>Unified error decoding (4xx → BusinessException, 5xx → SystemException)</li>
 *   <li>TraceId propagation via request headers</li>
 *   <li>Request/response logging</li>
 *   <li>Configurable timeouts (connection: 5000ms, read: 10000ms)</li>
 * </ul>
 * 
 * <p>Usage Example:
 * <pre>{@code
 * @FeignClient(
 *     name = "product-service",
 *     configuration = BaseFeignConfig.class,
 *     fallback = ProductClientFallback.class
 * )
 * public interface ProductClient {
 *     @GetMapping("/api/products/{id}")
 *     ProductDTO getProduct(@PathVariable Long id);
 * }
 * 
 * // Service using Feign client
 * @Service
 * public class OrderService {
 *     private final ProductClient productClient;
 *     
 *     public void validateProduct(Long productId) {
 *         try {
 *             ProductDTO product = productClient.getProduct(productId);
 *             // TraceId automatically propagated
 *         } catch (BusinessException e) {
 *             // Handle 4xx errors
 *             log.warn("商品不存在: {}", productId);
 *             throw e;
 *         } catch (SystemException e) {
 *             // Handle 5xx errors
 *             log.error("商品服务异常: {}", productId);
 *             throw e;
 *         }
 *     }
 * }
 * }</pre>
 * 
 * @author cuckoo-team
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "feign.codec.ErrorDecoder")
public class BaseFeignConfig {

    /**
     * Custom error decoder for unified error handling
     * 
     * @return ErrorDecoder instance
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    /**
     * Request interceptor for traceId propagation
     * 
     * @return RequestInterceptor instance
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new TraceIdRequestInterceptor();
    }

    /**
     * Configure Feign request options with timeouts
     * 
     * @return Request.Options with connection timeout 5000ms and read timeout 10000ms
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            5000,  // connection timeout in milliseconds
            10000  // read timeout in milliseconds
        );
    }

    /**
     * Custom error decoder implementation.
     * 
     * <p>Decodes HTTP error responses:
     * <ul>
     *   <li>4xx errors → BusinessException (client errors)</li>
     *   <li>5xx errors → SystemException (server errors)</li>
     *   <li>Other errors → default Feign handling</li>
     * </ul>
     */
    @Slf4j
    static class CustomErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultDecoder = new Default();

        @Override
        public Exception decode(String methodKey, Response response) {
            int status = response.status();
            String traceId = MDC.get("traceId");
            
            try {
                String errorBody = extractErrorBody(response);
                
                if (status >= 400 && status < 500) {
                    // 4xx errors → BusinessException
                    log.error("[{}] Feign 4xx 错误: method={}, status={}, body={}", 
                        traceId, methodKey, status, errorBody);
                    return new BusinessException(
                        String.format("业务错误: %s, 状态码: %d, 详情: %s", methodKey, status, errorBody));
                } else if (status >= 500) {
                    // 5xx errors → SystemException
                    log.error("[{}] Feign 5xx 错误: method={}, status={}, body={}", 
                        traceId, methodKey, status, errorBody);
                    return new SystemException(
                        String.format("系统错误: %s, 状态码: %d, 详情: %s", methodKey, status, errorBody));
                }
            } catch (IOException e) {
                log.error("[{}] 解析 Feign 错误响应失败: method={}", traceId, methodKey, e);
            }
            
            return defaultDecoder.decode(methodKey, response);
        }

        /**
         * Extracts error body from response
         * 
         * @param response Feign response
         * @return Error body as string, or "No error body" if not available
         * @throws IOException if reading response body fails
         */
        private String extractErrorBody(Response response) throws IOException {
            if (response.body() != null) {
                byte[] bodyData = response.body().asInputStream().readAllBytes();
                return new String(bodyData, StandardCharsets.UTF_8);
            }
            return "No error body";
        }
    }

    /**
     * Request interceptor for traceId propagation.
     * 
     * <p>Adds X-Trace-Id header to all outgoing Feign requests
     * and logs request details with traceId.
     */
    @Slf4j
    static class TraceIdRequestInterceptor implements RequestInterceptor {

        @Override
        public void apply(RequestTemplate template) {
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                template.header("X-Trace-Id", traceId);
                log.debug("[{}] Feign 请求: method={}, url={}", 
                    traceId, template.method(), template.url());
            }
        }
    }
}
