package com.pingxin403.cuckoo.order;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    io.opentelemetry.instrumentation.spring.autoconfigure.OpenTelemetryAutoConfiguration.class
})
class CuckooOrderApplicationTests {

    @Test
    void contextLoads() {
    }

}
