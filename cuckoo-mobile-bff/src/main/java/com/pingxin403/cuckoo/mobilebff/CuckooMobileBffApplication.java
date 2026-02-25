package com.pingxin403.cuckoo.mobilebff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Mobile BFF 应用启动类
 * 为移动端提供聚合服务
 */
@SpringBootApplication(scanBasePackages = {
    "com.pingxin403.cuckoo.mobilebff",
    "com.pingxin403.cuckoo.common"
})
@EnableDiscoveryClient
@EnableFeignClients
public class CuckooMobileBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuckooMobileBffApplication.class, args);
    }

}
