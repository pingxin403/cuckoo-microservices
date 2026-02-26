package com.pingxin403.cuckoo.payment;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test application class that excludes Kafka configuration
 * to allow clean mock injection for integration tests.
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.pingxin403.cuckoo.payment",
                "com.pingxin403.cuckoo.common"
        },
        exclude = {
                KafkaAutoConfiguration.class
        }
)
@ComponentScan(
        basePackages = {
                "com.pingxin403.cuckoo.payment",
                "com.pingxin403.cuckoo.common"
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CuckooPaymentApplication.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        com.pingxin403.cuckoo.common.audit.AuditLogAspect.class,
                        com.pingxin403.cuckoo.common.metrics.KafkaMetrics.class
                })
        }
)
@EnableJpaRepositories(basePackages = {
        "com.pingxin403.cuckoo.payment.repository",
        "com.pingxin403.cuckoo.common.idempotency"
})
@EntityScan(basePackages = {
        "com.pingxin403.cuckoo.payment.entity",
        "com.pingxin403.cuckoo.common.idempotency"
})
public class TestPaymentApplication {
}
