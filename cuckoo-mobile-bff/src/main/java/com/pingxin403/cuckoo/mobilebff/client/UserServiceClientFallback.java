package com.pingxin403.cuckoo.mobilebff.client;

import com.pingxin403.cuckoo.mobilebff.dto.UserInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户服务降级处理
 */
@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserInfoDTO getUserInfo(Long userId) {
        log.warn("User service fallback triggered for userId: {}", userId);
        UserInfoDTO fallbackUser = new UserInfoDTO();
        fallbackUser.setId(userId);
        fallbackUser.setUsername("用户" + userId);
        fallbackUser.setNickname("临时用户");
        return fallbackUser;
    }

}
