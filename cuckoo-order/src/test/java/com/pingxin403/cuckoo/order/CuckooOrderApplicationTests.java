package com.pingxin403.cuckoo.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = TestOrderApplication.class,
        properties = {
                "spring.kafka.bootstrap-servers=",
                "spring.cloud.openfeign.client.config.default.url=http://localhost"
        }
)
@ActiveProfiles("test")
class CuckooOrderApplicationTests {

    @Test
    void contextLoads() {
    }

}
