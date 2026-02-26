package com.pingxin403.cuckoo.order;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test application class that excludes Feign clients, Kafka consumers,
 * and scheduled jobs to allow clean mock injection for property tests.
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.pingxin403.cuckoo.order",
                "com.pingxin403.cuckoo.common"
        },
        exclude = {
                KafkaAutoConfiguration.class
        }
)
@ComponentScan(
        basePackages = {
                "com.pingxin403.cuckoo.order",
                "com.pingxin403.cuckoo.common"
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = FeignClient.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CuckooOrderApplication.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.pingxin403\\.cuckoo\\.order\\.consumer\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.pingxin403\\.cuckoo\\.order\\.job\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.pingxin403\\.cuckoo\\.order\\.client\\..*"),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        com.pingxin403.cuckoo.order.service.OrderReadModelUpdater.class,
                        com.pingxin403.cuckoo.order.service.OrderReadModelRepairService.class,
                        com.pingxin403.cuckoo.order.controller.OrderReadModelRepairController.class,
                        com.pingxin403.cuckoo.common.audit.AuditLogAspect.class
                })
        }
)
@EnableJpaRepositories(basePackages = {
        "com.pingxin403.cuckoo.order.repository",
        "com.pingxin403.cuckoo.common.idempotency"
})
@EntityScan(basePackages = {
        "com.pingxin403.cuckoo.order.entity",
        "com.pingxin403.cuckoo.common.idempotency"
})
public class TestOrderApplication {
}
