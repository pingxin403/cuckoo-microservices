package com.pingxin403.cuckoo.mobilebff.dto;

import lombok.Data;

/**
 * 用户信息 DTO（精简版）
 */
@Data
public class UserInfoDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
}
