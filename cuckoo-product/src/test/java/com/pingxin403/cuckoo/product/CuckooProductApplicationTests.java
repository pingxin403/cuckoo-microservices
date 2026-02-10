package com.pingxin403.cuckoo.product;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Context load test requires full infrastructure (Nacos, MySQL). Skipped for unit testing.")
class CuckooProductApplicationTests {

    @Test
    void contextLoads() {
    }

}
