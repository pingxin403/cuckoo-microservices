package com.pingxin403.cuckoo.user.service;

import com.pingxin403.cuckoo.common.exception.AuthenticationException;
import com.pingxin403.cuckoo.common.exception.DuplicateResourceException;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.user.dto.*;
import com.pingxin403.cuckoo.user.entity.User;
import com.pingxin403.cuckoo.user.mapper.UserMapper;
import com.pingxin403.cuckoo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

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
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserMapper userMapper;

    private static final String CACHE_KEY_PREFIX = "user:";
    private static final long CACHE_TTL_MINUTES = 15;

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

        return userMapper.toDTO(savedUser);
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
     * 实现 Cache-Aside Pattern：
     * 1. 先查询缓存
     * 2. 缓存命中则直接返回
     * 3. 缓存未命中则查询数据库
     * 4. 将查询结果写入缓存（TTL 15分钟）
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        
        // 1. 先查询缓存
        UserDTO cachedUser = (UserDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            log.debug("Cache hit for user: id={}", id);
            return cachedUser;
        }
        
        // 2. 缓存未命中，查询数据库
        log.debug("Cache miss for user: id={}, querying database", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        
        UserDTO userDTO = userMapper.toDTO(user);
        
        // 3. 将查询结果写入缓存，设置 TTL 为 15 分钟
        redisTemplate.opsForValue().set(cacheKey, userDTO, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        log.debug("User cached: id={}, ttl={}min", id, CACHE_TTL_MINUTES);
        
        return userDTO;
    }

    /**
     * 更新用户信息
     * 实现缓存更新策略：
     * 1. 先更新数据库
     * 2. 再删除缓存（Cache-Aside Pattern）
     */
    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        
        // 更新用户信息
        if (request.getEmail() != null) {
            // 检查新邮箱是否已被其他用户使用
            if (!request.getEmail().equals(user.getEmail()) && 
                userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User", "email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        
        // 1. 先更新数据库
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: id={}", id);
        
        // 2. 再删除缓存
        String cacheKey = CACHE_KEY_PREFIX + id;
        redisTemplate.delete(cacheKey);
        log.debug("User cache deleted: id={}", id);
        
        return userMapper.toDTO(updatedUser);
    }

    /**
     * 生成简单 Token
     * 格式：Base64(userId:username:timestamp)
     */
    private String generateSimpleToken(User user) {
        String raw = user.getId() + ":" + user.getUsername() + ":" + System.currentTimeMillis();
        return java.util.Base64.getEncoder().encodeToString(raw.getBytes());
    }
}
