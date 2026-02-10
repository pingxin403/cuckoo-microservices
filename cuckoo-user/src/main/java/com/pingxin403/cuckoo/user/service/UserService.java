package com.pingxin403.cuckoo.user.service;

import com.pingxin403.cuckoo.common.exception.AuthenticationException;
import com.pingxin403.cuckoo.common.exception.DuplicateResourceException;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.user.dto.*;
import com.pingxin403.cuckoo.user.entity.User;
import com.pingxin403.cuckoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务
 * 提供用户注册、登录和查询功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 用户注册
     * - 检查用户名和邮箱唯一性
     * - 使用 BCrypt 加密密码
     * - 返回用户信息（不含密码）
     */
    @Transactional
    public UserDTO register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // 创建用户，密码使用 BCrypt 加密
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: username={}, id={}", savedUser.getUsername(), savedUser.getId());

        return toDTO(savedUser);
    }

    /**
     * 用户登录
     * - 验证用户名和密码
     * - 生成简单 Token（格式：userId:username:timestamp）
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationException("Invalid username or password");
        }

        // 生成简单 Token（学习项目，非生产级别）
        String token = generateSimpleToken(user);
        log.info("User logged in successfully: username={}, id={}", user.getUsername(), user.getId());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .build();
    }

    /**
     * 根据 ID 查询用户
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toDTO(user);
    }

    /**
     * 生成简单 Token
     * 格式：Base64(userId:username:timestamp)
     */
    private String generateSimpleToken(User user) {
        String raw = user.getId() + ":" + user.getUsername() + ":" + System.currentTimeMillis();
        return java.util.Base64.getEncoder().encodeToString(raw.getBytes());
    }

    /**
     * 将 User 实体转换为 UserDTO（不含密码）
     */
    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
