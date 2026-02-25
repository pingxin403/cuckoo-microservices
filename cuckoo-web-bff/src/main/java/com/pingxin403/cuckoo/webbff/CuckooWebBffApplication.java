package com.pingxin403.cuckoo.webbff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Web BFF 应用启动类
 * 为 Web 端提供聚合服务
 */
@SpringBootApplication(scanBasePackages = {
    "com.pingxin403.cuckoo.webbff",
    "com.pingxin403.cuckoo.common"
})
@EnableDiscoveryClient
@EnableFeignClients
public class CuckooWebBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuckooWebBffApplication.class, args);
    }

}
