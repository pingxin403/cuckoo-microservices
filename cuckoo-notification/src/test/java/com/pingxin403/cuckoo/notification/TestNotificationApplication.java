package com.pingxin403.cuckoo.notification;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test application class that excludes Kafka consumers and config
 * to allow clean property-based testing without Kafka infrastructure.
 */
@SpringBootApplication(
        scanBasePackages = {
                "com.pingxin403.cuckoo.notification",
                "com.pingxin403.cuckoo.common"
        },
        exclude = {
                KafkaAutoConfiguration.class
        }
)
@ComponentScan(
        basePackages = {
                "com.pingxin403.cuckoo.notification",
                "com.pingxin403.cuckoo.common"
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CuckooNotificationApplication.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.pingxin403\\.cuckoo\\.notification\\.consumer\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.pingxin403\\.cuckoo\\.notification\\.config\\..*")
        }
)
@EnableJpaRepositories(basePackages = {
        "com.pingxin403.cuckoo.notification.repository",
        "com.pingxin403.cuckoo.common.idempotency"
})
@EntityScan(basePackages = {
        "com.pingxin403.cuckoo.notification.entity",
        "com.pingxin403.cuckoo.common.idempotency"
})
public class TestNotificationApplication {
}
