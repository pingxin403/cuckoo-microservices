package com.pingxin403.cuckoo.mobilebff.client;

import com.pingxin403.cuckoo.mobilebff.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 客户端
 */
@FeignClient(
    name = "cuckoo-user",
    path = "/api/users",
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {

    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    UserInfoDTO getUserInfo(@PathVariable("userId") Long userId);

}
