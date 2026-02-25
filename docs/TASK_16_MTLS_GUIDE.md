# Task 16: mTLS 服务间认证实施指南

## 概述

mTLS (Mutual TLS) 提供服务间双向认证，确保通信双方都经过验证。本文档提供两种实施方案：

1. **方案 A**: 使用 cert-manager + Spring Boot 原生支持（手动配置）
2. **方案 B**: 使用 Istio 服务网格（推荐，自动化）

## 方案 A: cert-manager + Spring Boot

### 16.1 生成服务证书

#### 1. 安装 cert-manager

```bash
# 安装 cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# 验证安装
kubectl get pods -n cert-manager
```

#### 2. 创建 CA Issuer

```yaml
# k8s/mtls/ca-issuer.yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: selfsigned-issuer
spec:
  selfSigned: {}
---
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: cuckoo-ca
  namespace: default
spec:
  isCA: true
  commonName: cuckoo-ca
  secretName: cuckoo-ca-secret
  privateKey:
    algorithm: RSA
    size: 4096
  issuerRef:
    name: selfsigned-issuer
    kind: ClusterIssuer
---
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: cuckoo-ca-issuer
spec:
  ca:
    secretName: cuckoo-ca-secret
```

#### 3. 为每个服务生成证书

```yaml
# k8s/mtls/order-service-certificate.yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: order-service-cert
  namespace: default
spec:
  secretName: order-service-tls
  duration: 2160h # 90 days
  renewBefore: 360h # 15 days
  subject:
    organizations:
      - cuckoo-microservices
  commonName: order-service.default.svc.cluster.local
  dnsNames:
    - order-service
    - order-service.default
    - order-service.default.svc
    - order-service.default.svc.cluster.local
  issuerRef:
    name: cuckoo-ca-issuer
    kind: ClusterIssuer
```

### 16.2 配置 Spring Boot 启用 mTLS

#### 1. 修改 application.yml

```yaml
# cuckoo-order/src/main/resources/application-mtls.yml
server:
  port: 8443
  ssl:
    enabled: true
    # 服务端证书
    key-store: file:/etc/certs/tls.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    # 客户端证书验证
    client-auth: need
    trust-store: file:/etc/certs/truststore.p12
    trust-store-password: ${TRUSTSTORE_PASSWORD}
    trust-store-type: PKCS12

# RestTemplate 配置
rest:
  client:
    ssl:
      key-store: file:/etc/certs/tls.p12
      key-store-password: ${KEYSTORE_PASSWORD}
      trust-store: file:/etc/certs/truststore.p12
      trust-store-password: ${TRUSTSTORE_PASSWORD}
```

#### 2. 创建 SSL 配置类

```java
// cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/security/MTLSConfig.java
package com.pingxin403.cuckoo.common.security;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;

@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class MTLSConfig {

    @Value("${rest.client.ssl.key-store}")
    private String keyStorePath;

    @Value("${rest.client.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${rest.client.ssl.trust-store}")
    private String trustStorePath;

    @Value("${rest.client.ssl.trust-store-password}")
    private String trustStorePassword;

    @Bean
    public RestTemplate mtlsRestTemplate() throws Exception {
        // 加载 KeyStore（客户端证书）
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keyStorePath)) {
            keyStore.load(fis, keyStorePassword.toCharArray());
        }

        // 加载 TrustStore（信任的 CA）
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(trustStorePath)) {
            trustStore.load(fis, trustStorePassword.toCharArray());
        }

        // 创建 SSLContext
        SSLContext sslContext = org.apache.hc.core5.ssl.SSLContexts.custom()
                .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
                .loadTrustMaterial(trustStore, null)
                .build();

        // 创建 HttpClient
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(socketFactory)
                                .build()
                )
                .build();

        HttpComponentsClientHttpRequestFactory factory = 
                new HttpComponentsClientHttpRequestFactory(httpClient);
        
        return new RestTemplate(factory);
    }
}
```

### 16.3 配置证书验证和告警

#### 1. 证书验证拦截器

```java
// cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/security/CertificateValidationFilter.java
package com.pingxin403.cuckoo.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.cert.X509Certificate;

@Slf4j
@Component
@ConditionalOnProperty(name = "server.ssl.client-auth", havingValue = "need")
public class CertificateValidationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute(
                "jakarta.servlet.request.X509Certificate");

        if (certs == null || certs.length == 0) {
            log.error("No client certificate provided");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
                    "Client certificate required");
            return;
        }

        X509Certificate clientCert = certs[0];
        
        // 验证证书有效期
        try {
            clientCert.checkValidity();
        } catch (Exception e) {
            log.error("Client certificate is invalid: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, 
                    "Invalid client certificate");
            return;
        }

        // 提取客户端信息
        String clientDN = clientCert.getSubjectX500Principal().getName();
        log.info("Client authenticated: {}", clientDN);

        filterChain.doFilter(request, response);
    }
}
```

#### 2. 证书过期监控

```java
// cuckoo-common/src/main/java/com/pingxin403/cuckoo/common/security/CertificateExpiryMonitor.java
package com.pingxin403.cuckoo.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;

@Slf4j
@Component
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class CertificateExpiryMonitor {

    @Value("${server.ssl.key-store}")
    private String keyStorePath;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    private static final int WARNING_DAYS = 30;

    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点检查
    public void checkCertificateExpiry() {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                keyStore.load(fis, keyStorePassword.toCharArray());
            }

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                
                if (cert != null) {
                    Instant expiry = cert.getNotAfter().toInstant();
                    long daysUntilExpiry = ChronoUnit.DAYS.between(Instant.now(), expiry);
                    
                    if (daysUntilExpiry < 0) {
                        log.error("Certificate {} has EXPIRED!", alias);
                        // TODO: 发送告警
                    } else if (daysUntilExpiry < WARNING_DAYS) {
                        log.warn("Certificate {} will expire in {} days", alias, daysUntilExpiry);
                        // TODO: 发送告警
                    } else {
                        log.info("Certificate {} is valid for {} days", alias, daysUntilExpiry);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to check certificate expiry", e);
        }
    }
}
```

### 16.4 Kubernetes 部署配置

```yaml
# k8s/services/order-service-deployment-mtls.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  template:
    spec:
      containers:
      - name: order-service
        image: cuckoo/order-service:latest
        ports:
        - containerPort: 8443
          name: https
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod,mtls"
        - name: KEYSTORE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: order-service-keystore-password
              key: password
        - name: TRUSTSTORE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: order-service-truststore-password
              key: password
        volumeMounts:
        - name: certs
          mountPath: /etc/certs
          readOnly: true
      volumes:
      - name: certs
        secret:
          secretName: order-service-tls
---
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  ports:
  - port: 8443
    targetPort: 8443
    protocol: TCP
    name: https
  selector:
    app: order-service
```

---

## 方案 B: Istio 服务网格（推荐）

### 优势

- 自动证书管理和轮换
- 无需修改应用代码
- 统一的安全策略
- 更好的可观测性

### 实施步骤

#### 1. 安装 Istio

```bash
# 下载 Istio
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH=$PWD/bin:$PATH

# 安装 Istio
istioctl install --set profile=default -y

# 启用自动注入
kubectl label namespace default istio-injection=enabled
```

#### 2. 启用 mTLS

```yaml
# k8s/istio/peer-authentication.yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: default
spec:
  mtls:
    mode: STRICT  # 强制 mTLS
```

#### 3. 配置目标规则

```yaml
# k8s/istio/destination-rule.yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: default
  namespace: default
spec:
  host: "*.default.svc.cluster.local"
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL  # 使用 Istio 管理的 mTLS
```

#### 4. 验证 mTLS

```bash
# 检查 mTLS 状态
istioctl authn tls-check order-service.default.svc.cluster.local

# 查看证书
kubectl exec -it <pod-name> -c istio-proxy -- \
  openssl s_client -showcerts -connect order-service:8084
```

---

## 测试和验证

### 测试 mTLS 连接

```bash
# 使用 curl 测试（需要客户端证书）
curl --cert /path/to/client.crt \
     --key /path/to/client.key \
     --cacert /path/to/ca.crt \
     https://order-service:8443/actuator/health

# 预期结果：成功返回健康状态
```

### 测试证书验证失败

```bash
# 不提供客户端证书
curl --cacert /path/to/ca.crt \
     https://order-service:8443/actuator/health

# 预期结果：401 Unauthorized
```

---

## 监控和告警

### Prometheus 指标

```yaml
# 证书过期时间（天）
certificate_expiry_days{service="order-service"} 45

# 证书验证失败次数
certificate_validation_failures_total{service="order-service"} 0
```

### Grafana 面板

- 证书过期时间趋势
- 证书验证失败率
- mTLS 连接成功率

---

## 故障排查

### 常见问题

1. **证书验证失败**
   - 检查证书是否过期
   - 检查 CA 证书是否正确
   - 检查证书 CN/SAN 是否匹配

2. **连接超时**
   - 检查端口是否正确（HTTPS 通常是 8443）
   - 检查防火墙规则
   - 检查 Service 配置

3. **证书加载失败**
   - 检查文件路径是否正确
   - 检查文件权限
   - 检查密码是否正确

---

## 最佳实践

1. **使用 Istio 服务网格**：自动化证书管理，减少运维负担
2. **定期轮换证书**：建议 90 天轮换一次
3. **监控证书过期**：提前 30 天发送告警
4. **使用强加密算法**：RSA 4096 或 ECDSA P-256
5. **限制证书权限**：最小权限原则
6. **审计日志**：记录所有证书操作

---

## 总结

- **方案 A** 适合小规模部署或特殊需求
- **方案 B** 适合生产环境，推荐使用
- mTLS 提供强大的安全保障，但增加了复杂度
- 在内网环境中，可以考虑使用 VPN 或其他网络隔离方案替代

---

## 参考资料

- [cert-manager 文档](https://cert-manager.io/docs/)
- [Istio mTLS 文档](https://istio.io/latest/docs/tasks/security/authentication/mtls-migration/)
- [Spring Boot SSL 配置](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.configure-ssl)
