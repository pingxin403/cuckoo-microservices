# Task 17: 增强 RBAC 权限控制实施指南

## 概述

基于角色的访问控制（RBAC）提供细粒度的权限管理。本文档提供完整的 RBAC 实施方案，包括 JWT 角色提取、授权过滤器、权限配置和动态刷新。

---

## 17.1 实现 JWT 角色提取

### 1. JWT Token 结构

```json
{
  "sub": "user123",
  "username": "john.doe",
  "roles": ["USER", "ADMIN"],
  "permissions": ["order:create", "order:read", "product:read"],
  "exp": 1234567890,
  "iat": 1234567890
}
```

### 2. JWT Token Provider

```java
// cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/security/JwtTokenProvider.java
package com.pingxin403.cuckoo.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 默认 24 小时
    private long expiration;

    /**
     * 生成 JWT Token（包含角色）
     */
    public String generateToken(String username, Long userId, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 从 Token 中提取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从 Token 中提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从 Token 中提取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("roles", List.class);
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析 Token
     */
    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
```

### 3. 用户角色枚举

```java
// cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/security/Role.java
package com.pingxin403.cuckoo.common.security;

public enum Role {
    ADMIN("管理员", "所有权限"),
    USER("普通用户", "基本操作权限"),
    GUEST("访客", "只读权限");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
```

---

## 17.2 实现 RBAC 授权过滤器

### 1. RBAC 授权过滤器

```java
// cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/security/RBACAuthorizationFilter.java
package com.pingxin403.cuckoo.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RBACAuthorizationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final PermissionRegistry permissionRegistry;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 跳过公开端点
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 提取 JWT Token
        String token = extractToken(request);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            sendUnauthorizedResponse(response, "Invalid or missing token");
            return;
        }

        // 提取用户角色
        List<String> userRoles = jwtTokenProvider.getRolesFromToken(token);
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // 检查权限
        List<String> requiredRoles = permissionRegistry.getRequiredRoles(path, method);
        if (requiredRoles.isEmpty()) {
            // 没有配置权限要求，允许通过
            filterChain.doFilter(request, response);
            return;
        }

        // 验证用户是否拥有所需角色
        boolean hasPermission = userRoles.stream()
                .anyMatch(requiredRoles::contains);

        if (!hasPermission) {
            log.warn("Access denied for user {} to {} {}, required roles: {}, user roles: {}",
                    username, method, path, requiredRoles, userRoles);
            sendForbiddenResponse(response, "Insufficient permissions");
            return;
        }

        log.debug("Access granted for user {} to {} {}", username, method, path);
        
        // 将用户信息存入请求属性
        request.setAttribute("userId", jwtTokenProvider.getUserIdFromToken(token));
        request.setAttribute("username", username);
        request.setAttribute("roles", userRoles);

        filterChain.doFilter(request, response);
    }

    /**
     * 判断是否为公开端点
     */
    private boolean isPublicEndpoint(String path) {
        List<String> publicPaths = List.of(
                "/actuator/**",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/auth/login",
                "/auth/register"
        );

        return publicPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 从请求头中提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 发送 401 未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> errorResponse = Map.of(
                "error", "Unauthorized",
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * 发送 403 禁止访问响应
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> errorResponse = Map.of(
                "error", "Forbidden",
                "message", message,
                "timestamp", System.currentTimeMillis()
        );
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
```

---

## 17.3 配置 API 端点权限

### 1. 权限注册表

```java
// cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/security/PermissionRegistry.java
package com.pingxin403.cuckoo.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PermissionRegistry {

    private final Map<String, List<String>> permissions = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public PermissionRegistry() {
        initializeDefaultPermissions();
    }

    /**
     * 初始化默认权限配置
     */
    private void initializeDefaultPermissions() {
        // 管理员权限（所有操作）
        registerPermission("DELETE:/api/orders/**", List.of("ADMIN"));
        registerPermission("DELETE:/api/products/**", List.of("ADMIN"));
        registerPermission("PUT:/api/products/**", List.of("ADMIN"));
        registerPermission("POST:/api/products", List.of("ADMIN"));

        // 普通用户权限（查询和创建订单）
        registerPermission("GET:/api/orders/**", List.of("USER", "ADMIN"));
        registerPermission("POST:/api/orders", List.of("USER", "ADMIN"));
        registerPermission("PUT:/api/orders/*/cancel", List.of("USER", "ADMIN"));

        // 访客权限（查询商品）
        registerPermission("GET:/api/products/**", List.of("GUEST", "USER", "ADMIN"));

        log.info("Initialized {} permission rules", permissions.size());
    }

    /**
     * 注册权限规则
     */
    public void registerPermission(String endpoint, List<String> roles) {
        permissions.put(endpoint, new ArrayList<>(roles));
        log.debug("Registered permission: {} -> {}", endpoint, roles);
    }

    /**
     * 获取端点所需角色
     */
    public List<String> getRequiredRoles(String path, String method) {
        String key = method + ":" + path;

        // 精确匹配
        if (permissions.containsKey(key)) {
            return permissions.get(key);
        }

        // 模式匹配
        for (Map.Entry<String, List<String>> entry : permissions.entrySet()) {
            String pattern = entry.getKey();
            if (matchesPattern(pattern, key)) {
                return entry.getValue();
            }
        }

        return Collections.emptyList();
    }

    /**
     * 模式匹配
     */
    private boolean matchesPattern(String pattern, String path) {
        return pathMatcher.match(pattern, path);
    }

    /**
     * 更新权限配置（用于动态刷新）
     */
    public void updatePermissions(Map<String, List<String>> newPermissions) {
        permissions.clear();
        permissions.putAll(newPermissions);
        log.info("Updated permissions, total rules: {}", permissions.size());
    }

    /**
     * 获取所有权限配置
     */
    public Map<String, List<String>> getAllPermissions() {
        return new HashMap<>(permissions);
    }
}
```

### 2. 权限配置示例

```yaml
# Nacos 配置中心 - rbac-permissions.yml
permissions:
  # 订单管理
  - endpoint: "DELETE:/api/orders/**"
    roles: ["ADMIN"]
  - endpoint: "GET:/api/orders/**"
    roles: ["USER", "ADMIN"]
  - endpoint: "POST:/api/orders"
    roles: ["USER", "ADMIN"]
  - endpoint: "PUT:/api/orders/*/cancel"
    roles: ["USER", "ADMIN"]
  
  # 商品管理
  - endpoint: "GET:/api/products/**"
    roles: ["GUEST", "USER", "ADMIN"]
  - endpoint: "POST:/api/products"
    roles: ["ADMIN"]
  - endpoint: "PUT:/api/products/**"
    roles: ["ADMIN"]
  - endpoint: "DELETE:/api/products/**"
    roles: ["ADMIN"]
  
  # 用户管理
  - endpoint: "GET:/api/users/**"
    roles: ["ADMIN"]
  - endpoint: "PUT:/api/users/**"
    roles: ["ADMIN"]
  - endpoint: "DELETE:/api/users/**"
    roles: ["ADMIN"]
```

---

## 17.4 实现权限动态刷新

### 1. Nacos 配置监听器

```java
// cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/security/PermissionConfigListener.java
package com.pingxin403.cuckoo.common.security;

import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionConfigListener {

    private final PermissionRegistry permissionRegistry;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * 监听 Nacos 配置变更
     */
    @NacosConfigListener(dataId = "rbac-permissions.yml", groupId = "DEFAULT_GROUP")
    public void onConfigChange(String configInfo) {
        try {
            log.info("Received permission config update from Nacos");
            
            // 解析配置
            PermissionConfig config = yamlMapper.readValue(configInfo, PermissionConfig.class);
            
            // 转换为权限映射
            Map<String, List<String>> newPermissions = config.getPermissions().stream()
                    .collect(Collectors.toMap(
                            PermissionRule::getEndpoint,
                            PermissionRule::getRoles
                    ));
            
            // 更新权限注册表
            permissionRegistry.updatePermissions(newPermissions);
            
            log.info("Successfully updated {} permission rules", newPermissions.size());
        } catch (Exception e) {
            log.error("Failed to update permission config", e);
        }
    }

    /**
     * 权限配置类
     */
    private static class PermissionConfig {
        private List<PermissionRule> permissions;

        public List<PermissionRule> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<PermissionRule> permissions) {
            this.permissions = permissions;
        }
    }

    /**
     * 权限规则类
     */
    private static class PermissionRule {
        private String endpoint;
        private List<String> roles;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}
```

### 2. 权限管理 REST API

```java
// cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/security/PermissionController.java
package com.pingxin403.cuckoo.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRegistry permissionRegistry;

    /**
     * 获取所有权限配置
     */
    @GetMapping
    public ResponseEntity<Map<String, List<String>>> getAllPermissions() {
        return ResponseEntity.ok(permissionRegistry.getAllPermissions());
    }

    /**
     * 查询特定端点的权限要求
     */
    @GetMapping("/check")
    public ResponseEntity<List<String>> checkPermission(
            @RequestParam String path,
            @RequestParam String method) {
        List<String> roles = permissionRegistry.getRequiredRoles(path, method);
        return ResponseEntity.ok(roles);
    }

    /**
     * 添加权限规则（需要 ADMIN 角色）
     */
    @PostMapping
    public ResponseEntity<Void> addPermission(
            @RequestParam String endpoint,
            @RequestBody List<String> roles) {
        permissionRegistry.registerPermission(endpoint, roles);
        return ResponseEntity.ok().build();
    }
}
```

---

## 配置和集成

### 1. application.yml 配置

```yaml
# JWT 配置
jwt:
  secret: ${JWT_SECRET:your-secret-key-change-this-in-production}
  expiration: 86400000  # 24 小时

# Nacos 配置
spring:
  cloud:
    nacos:
      config:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:public}
        group: DEFAULT_GROUP
        file-extension: yml
        extension-configs:
          - data-id: rbac-permissions.yml
            group: DEFAULT_GROUP
            refresh: true
```

### 2. Security 配置类

```java
// cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/security/SecurityConfig.java
package com.pingxin403.cuckoo.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final RBACAuthorizationFilter rbacAuthorizationFilter;

    @Bean
    public FilterRegistrationBean<RBACAuthorizationFilter> rbacFilter() {
        FilterRegistrationBean<RBACAuthorizationFilter> registrationBean = 
                new FilterRegistrationBean<>();
        
        registrationBean.setFilter(rbacAuthorizationFilter);
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);
        
        return registrationBean;
    }
}
```

---

## 测试和验证

### 1. 单元测试

```java
// cuckoo-common/src/test/java/com/pingxin403/cuckoo/common/security/RBACAuthorizationFilterTest.java
package com.pingxin403.cuckoo.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RBACAuthorizationFilterTest {

    @Test
    void testAdminCanAccessAllEndpoints() {
        // 测试管理员可以访问所有端点
    }

    @Test
    void testUserCannotDeleteOrders() {
        // 测试普通用户不能删除订单
    }

    @Test
    void testGuestCanOnlyReadProducts() {
        // 测试访客只能查询商品
    }
}
```

### 2. 集成测试

```bash
# 测试管理员权限
curl -H "Authorization: Bearer <admin-token>" \
     -X DELETE http://localhost:8084/api/orders/123

# 预期结果：200 OK

# 测试普通用户权限
curl -H "Authorization: Bearer <user-token>" \
     -X DELETE http://localhost:8084/api/orders/123

# 预期结果：403 Forbidden
```

---

## 监控和审计

### 1. 权限检查指标

```java
// 在 RBACAuthorizationFilter 中添加指标
@Autowired
private MeterRegistry meterRegistry;

private void recordAccessDenied(String username, String path, String method) {
    meterRegistry.counter("rbac.access.denied",
            "username", username,
            "path", path,
            "method", method
    ).increment();
}
```

### 2. 审计日志

所有权限检查失败都会记录到审计日志中，便于安全审计和问题排查。

---

## 最佳实践

1. **最小权限原则**：只授予必要的权限
2. **角色分层**：GUEST < USER < ADMIN
3. **定期审计**：定期检查权限配置
4. **动态刷新**：使用 Nacos 配置中心动态更新权限
5. **监控告警**：监控权限拒绝次数，异常时告警
6. **测试覆盖**：为每个角色编写测试用例

---

## 总结

RBAC 权限控制提供了灵活的权限管理机制，通过 JWT 角色提取、授权过滤器和动态配置，可以实现细粒度的访问控制。结合审计日志和监控，可以确保系统的安全性和可追溯性。
