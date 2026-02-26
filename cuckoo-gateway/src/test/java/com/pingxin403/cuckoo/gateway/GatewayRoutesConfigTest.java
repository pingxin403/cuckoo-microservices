package com.pingxin403.cuckoo.gateway;

import com.pingxin403.cuckoo.gateway.config.ReactiveTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 API 网关路由配置
 * 验证所有微服务的路由规则是否正确配置
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(ReactiveTestConfig.class)
class GatewayRoutesConfigTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void testAllRoutesAreConfigured() {
        // 获取所有路由定义
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        assertThat(routes).isNotNull();
        
        // 将路由转换为 Map 以便验证
        Map<String, RouteDefinition> routeMap = routes.stream()
                .collect(Collectors.toMap(RouteDefinition::getId, r -> r));

        // 验证所有必需的路由都已配置
        assertThat(routeMap).containsKeys(
                "user-service",
                "product-service",
                "inventory-service",
                "order-service",
                "payment-service",
                "notification-service"
        );
    }

    @Test
    void testUserServiceRoute() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        RouteDefinition userRoute = routes.stream()
                .filter(r -> "user-service".equals(r.getId()))
                .findFirst()
                .orElse(null);

        assertThat(userRoute).isNotNull();
        assertThat(userRoute.getUri().toString()).isEqualTo("lb://user-service");
        assertThat(userRoute.getPredicates()).isNotEmpty();
        
        // 验证路径谓词
        String pathPattern = userRoute.getPredicates().get(0).getArgs().get("_genkey_0");
        assertThat(pathPattern).isEqualTo("/api/users/**");
    }

    @Test
    void testProductServiceRoute() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        RouteDefinition productRoute = routes.stream()
                .filter(r -> "product-service".equals(r.getId()))
                .findFirst()
                .orElse(null);

        assertThat(productRoute).isNotNull();
        assertThat(productRoute.getUri().toString()).isEqualTo("lb://product-service");
        assertThat(productRoute.getPredicates()).isNotEmpty();
        
        String pathPattern = productRoute.getPredicates().get(0).getArgs().get("_genkey_0");
        assertThat(pathPattern).isEqualTo("/api/products/**");
    }

    @Test
    void testInventoryServiceRoute() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        RouteDefinition inventoryRoute = routes.stream()
                .filter(r -> "inventory-service".equals(r.getId()))
                .findFirst()
                .orElse(null);

        assertThat(inventoryRoute).isNotNull();
        assertThat(inventoryRoute.getUri().toString()).isEqualTo("lb://inventory-service");
        assertThat(inventoryRoute.getPredicates()).isNotEmpty();
        
        String pathPattern = inventoryRoute.getPredicates().get(0).getArgs().get("_genkey_0");
        assertThat(pathPattern).isEqualTo("/api/inventory/**");
    }

    @Test
    void testOrderServiceRoute() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        RouteDefinition orderRoute = routes.stream()
                .filter(r -> "order-service".equals(r.getId()))
                .findFirst()
                .orElse(null);

        assertThat(orderRoute).isNotNull();
        assertThat(orderRoute.getUri().toString()).isEqualTo("lb://order-service");
        assertThat(orderRoute.getPredicates()).isNotEmpty();
        
        String pathPattern = orderRoute.getPredicates().get(0).getArgs().get("_genkey_0");
        assertThat(pathPattern).isEqualTo("/api/orders/**");
    }

    @Test
    void testPaymentServiceRoute() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        RouteDefinition paymentRoute = routes.stream()
                .filter(r -> "payment-service".equals(r.getId()))
                .findFirst()
                .orElse(null);

        assertThat(paymentRoute).isNotNull();
        assertThat(paymentRoute.getUri().toString()).isEqualTo("lb://payment-service");
        assertThat(paymentRoute.getPredicates()).isNotEmpty();
        
        String pathPattern = paymentRoute.getPredicates().get(0).getArgs().get("_genkey_0");
        assertThat(pathPattern).isEqualTo("/api/payments/**");
    }

    @Test
    void testNotificationServiceRoute() {
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        RouteDefinition notificationRoute = routes.stream()
                .filter(r -> "notification-service".equals(r.getId()))
                .findFirst()
                .orElse(null);

        assertThat(notificationRoute).isNotNull();
        assertThat(notificationRoute.getUri().toString()).isEqualTo("lb://notification-service");
        assertThat(notificationRoute.getPredicates()).isNotEmpty();
        
        String pathPattern = notificationRoute.getPredicates().get(0).getArgs().get("_genkey_0");
        assertThat(pathPattern).isEqualTo("/api/notifications/**");
    }

    @Test
    void testLoadBalancerIsEnabled() {
        // 验证所有路由都使用 lb:// 前缀（负载均衡）
        List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        assertThat(routes).isNotNull();
        
        routes.stream()
                .filter(r -> r.getId().endsWith("-service"))
                .forEach(route -> {
                    assertThat(route.getUri().getScheme())
                            .as("Route %s should use load balancer", route.getId())
                            .isEqualTo("lb");
                });
    }
}
