package com.pingxin403.cuckoo.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Application context loading test.
 * Only runs when database is available (integration test environment).
 */
@EnabledIfEnvironmentVariable(named = "INTEGRATION_TEST", matches = "true")
class CuckooUserApplicationTests {

    @Test
    void contextLoads() {
    }

}
