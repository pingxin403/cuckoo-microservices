package com.pingxin403.cuckoo.user.service;

import com.pingxin403.cuckoo.common.exception.DuplicateResourceException;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.user.dto.RegisterRequest;
import com.pingxin403.cuckoo.user.dto.UserDTO;
import com.pingxin403.cuckoo.user.entity.User;
import com.pingxin403.cuckoo.user.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

/**
 * UserService 属性测试
 * 使用 jqwik 框架验证用户服务的正确性属性
 * 
 * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
@Import(com.pingxin403.cuckoo.user.config.TestConfig.class)
class UserServicePropertyTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 属性测试：用户注册 - 验证用户名唯一性
     * 
     * 对于任意有效的注册请求，如果用户名已存在，则应该抛出 DuplicateResourceException
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("User registration enforces username uniqueness")
    @Transactional
    void userRegistration_enforcesUsernameUniqueness(
            @ForAll("validRegisterRequests") RegisterRequest firstRequest,
            @ForAll("validRegisterRequests") RegisterRequest secondRequest) {
        
        // Assume: Ensure the two requests have different emails
        Assume.that(!firstRequest.getEmail().equals(secondRequest.getEmail()));
        
        // Clean database
        userRepository.deleteAll();
        
        // Arrange: Register first user
        UserDTO firstUser = userService.register(firstRequest);
        
        // Assert: First registration should succeed
        assertThat(firstUser).isNotNull();
        assertThat(firstUser.getUsername()).isEqualTo(firstRequest.getUsername());
        assertThat(firstUser.getEmail()).isEqualTo(firstRequest.getEmail());
        
        // Arrange: Create second request with same username but different email
        RegisterRequest duplicateUsernameRequest = new RegisterRequest(
            firstRequest.getUsername(),  // Same username
            secondRequest.getEmail(),    // Different email
            secondRequest.getPassword()
        );
        
        // Act & Assert: Second registration with duplicate username should fail
        assertThatThrownBy(() -> userService.register(duplicateUsernameRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("username");
    }

    /**
     * 属性测试：用户注册 - 验证邮箱唯一性
     * 
     * 对于任意有效的注册请求，如果邮箱已存在，则应该抛出 DuplicateResourceException
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("User registration enforces email uniqueness")
    @Transactional
    void userRegistration_enforcesEmailUniqueness(
            @ForAll("validRegisterRequests") RegisterRequest firstRequest,
            @ForAll("validRegisterRequests") RegisterRequest secondRequest) {
        
        // Assume: Ensure the two requests have different usernames
        Assume.that(!firstRequest.getUsername().equals(secondRequest.getUsername()));
        
        // Clean database
        userRepository.deleteAll();
        
        // Arrange: Register first user
        UserDTO firstUser = userService.register(firstRequest);
        
        // Assert: First registration should succeed
        assertThat(firstUser).isNotNull();
        
        // Arrange: Create second request with same email but different username
        RegisterRequest duplicateEmailRequest = new RegisterRequest(
            secondRequest.getUsername(),  // Different username
            firstRequest.getEmail(),      // Same email
            secondRequest.getPassword()
        );
        
        // Act & Assert: Second registration with duplicate email should fail
        assertThatThrownBy(() -> userService.register(duplicateEmailRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("email");
    }

    /**
     * 属性测试：用户注册 - 验证密码加密
     * 
     * 对于任意有效的注册请求，密码应该被加密存储，不应该以明文形式存储
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("User registration encrypts password")
    @Transactional
    void userRegistration_encryptsPassword(
            @ForAll("validRegisterRequests") RegisterRequest request) {
        
        // Clean database
        userRepository.deleteAll();
        
        // Act: Register user
        UserDTO userDTO = userService.register(request);
        
        // Assert: User should be created
        assertThat(userDTO).isNotNull();
        assertThat(userDTO.getId()).isNotNull();
        
        // Assert: Password should be encrypted in database
        User savedUser = userRepository.findById(userDTO.getId()).orElseThrow();
        assertThat(savedUser.getPassword()).isNotEqualTo(request.getPassword());
        assertThat(savedUser.getPassword()).startsWith("$2a$");  // BCrypt prefix
        
        // Assert: Encrypted password should match original password
        assertThat(passwordEncoder.matches(request.getPassword(), savedUser.getPassword())).isTrue();
    }

    /**
     * 属性测试：用户查询 - 验证查询结果正确性
     * 
     * 对于任意注册的用户，通过 ID 查询应该返回正确的用户信息
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("User query returns correct user information")
    @Transactional
    void userQuery_returnsCorrectUserInformation(
            @ForAll("validRegisterRequests") RegisterRequest request) {
        
        // Clean database
        userRepository.deleteAll();
        
        // Arrange: Register user
        UserDTO registeredUser = userService.register(request);
        
        // Act: Query user by ID
        UserDTO queriedUser = userService.getUserById(registeredUser.getId());
        
        // Assert: Queried user should match registered user
        assertThat(queriedUser).isNotNull();
        assertThat(queriedUser.getId()).isEqualTo(registeredUser.getId());
        assertThat(queriedUser.getUsername()).isEqualTo(registeredUser.getUsername());
        assertThat(queriedUser.getEmail()).isEqualTo(registeredUser.getEmail());
        assertThat(queriedUser.getCreatedAt()).isEqualTo(registeredUser.getCreatedAt());
        assertThat(queriedUser.getUpdatedAt()).isEqualTo(registeredUser.getUpdatedAt());
    }

    /**
     * 属性测试：用户查询 - 验证不存在的用户抛出异常
     * 
     * 对于任意不存在的用户 ID，查询应该抛出 ResourceNotFoundException
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("User query throws exception for non-existent user")
    @Transactional
    void userQuery_throwsExceptionForNonExistentUser(
            @ForAll("positiveIds") Long nonExistentId) {
        
        // Clean database
        userRepository.deleteAll();
        
        // Assume: User with this ID does not exist
        Assume.that(!userRepository.existsById(nonExistentId));
        
        // Act & Assert: Query should throw ResourceNotFoundException
        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User");
    }

    /**
     * 属性测试：用户注册 - 验证返回的 DTO 不包含密码
     * 
     * 对于任意有效的注册请求，返回的 UserDTO 不应该包含密码信息
     * 
     * **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
     */
    @Property(tries = 100)
    @Label("User registration returns DTO without password")
    @Transactional
    void userRegistration_returnsDTOWithoutPassword(
            @ForAll("validRegisterRequests") RegisterRequest request) {
        
        // Clean database
        userRepository.deleteAll();
        
        // Act: Register user
        UserDTO userDTO = userService.register(request);
        
        // Assert: UserDTO should not contain password
        assertThat(userDTO).isNotNull();
        assertThat(userDTO.getUsername()).isEqualTo(request.getUsername());
        assertThat(userDTO.getEmail()).isEqualTo(request.getEmail());
        
        // UserDTO class doesn't have password field, so this is guaranteed by design
        // But we verify the returned data doesn't accidentally include it
        assertThat(userDTO.toString()).doesNotContain(request.getPassword());
    }

    // ========== Data Generators ==========

    /**
     * 生成有效的用户注册请求
     * 
     * 用户名：3-20 个字母字符
     * 邮箱：有效的邮箱格式
     * 密码：6-20 个字符
     */
    @Provide
    Arbitrary<RegisterRequest> validRegisterRequests() {
        Arbitrary<String> usernames = Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(3)
            .ofMaxLength(20);
        
        Arbitrary<String> emails = Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(3)
            .ofMaxLength(10)
            .map(localPart -> localPart + "@example.com");
        
        Arbitrary<String> passwords = Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(6)
            .ofMaxLength(20);
        
        return Combinators.combine(usernames, emails, passwords)
            .as(RegisterRequest::new);
    }

    /**
     * 生成正整数 ID
     */
    @Provide
    Arbitrary<Long> positiveIds() {
        return Arbitraries.longs().between(1L, 999999L);
    }
}
