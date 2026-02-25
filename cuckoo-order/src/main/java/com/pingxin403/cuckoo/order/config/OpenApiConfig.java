package com.pingxin403.cuckoo.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 配置
 * 自动生成 API 文档
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:cuckoo-order}")
    private String applicationName;

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cuckoo Order Service API")
                        .description("""
                                订单服务 API 文档
                                
                                ## 功能特性
                                - 订单创建：支持创建新订单
                                - 订单查询：支持多种查询方式（ID、用户、状态）
                                - 订单取消：支持取消订单
                                - CQRS模式：读写分离，优化查询性能
                                
                                ## 技术栈
                                - Spring Boot 3.2.5
                                - Spring Cloud Alibaba
                                - MySQL + Redis
                                - Kafka 事件驱动
                                - OpenTelemetry 链路追踪
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Cuckoo Team")
                                .email("support@cuckoo.com")
                                .url("https://cuckoo.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8084")
                                .description("本地开发环境"),
                        new Server()
                                .url("http://order-service:8084")
                                .description("Kubernetes 集群环境")
                ));
    }
}
