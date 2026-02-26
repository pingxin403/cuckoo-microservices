package com.pingxin403.cuckoo.gateway;

import com.pingxin403.cuckoo.gateway.config.ReactiveTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(ReactiveTestConfig.class)
class CuckooGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
