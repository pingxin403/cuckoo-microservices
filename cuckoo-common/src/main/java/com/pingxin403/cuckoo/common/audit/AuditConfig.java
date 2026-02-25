package com.pingxin403.cuckoo.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 审计日志配置
 */
@Configuration
@EnableAspectJAutoProxy
@EnableAsync
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditConfig {

    @Bean
    public ObjectMapper auditObjectMapper() {
        return new ObjectMapper();
    }
}
