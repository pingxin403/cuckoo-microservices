package com.pingxin403.cuckoo.user.service;

import com.pingxin403.cuckoo.common.exception.AuthenticationException;
import com.pingxin403.cuckoo.common.exception.DuplicateResourceException;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.user.dto.*;
import com.pingxin403.cuckoo.user.entity.User;
import com.pingxin403.cuckoo.user.mapper.UserMapper;
import com.pingxin403.cuckoo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Mock
    private org.springframework.data.redis.core.ValueOperations<String, Object> valueOperations;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("$2a$10$encodedPassword")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        testUserDTO = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .createdAt(testUser.getCreatedAt())
                .updatedAt(testUser.getUpdatedAt())
                .build();
        
        // Mock RedisTemplate behavior
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Mock UserMapper behavior
        when(userMapper.toDTO(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user == null) return null;
            return UserDTO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
        });
    }

    // ========== Register Tests ==========

    @Test
    @DisplayName("register - should create user and return UserDTO without password")
    void register_success() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDTO result = userService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register - should throw DuplicateResourceException when username exists")
    void register_duplicateUsername() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("username");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - should throw DuplicateResourceException when email exists")
    void register_duplicateEmail() {
        RegisterRequest request = new RegisterRequest("newuser", "test@example.com", "password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

    // ========== Login Tests ==========

    @Test
    @DisplayName("login - should return token and userId on valid credentials")
    void login_success() {
        LoginRequest request = new LoginRequest("testuser", "password123");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "$2a$10$encodedPassword")).thenReturn(true);

        LoginResponse result = userService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getToken()).isNotBlank();

        // Verify token contains userId
        String decoded = new String(java.util.Base64.getDecoder().decode(result.getToken()));
        assertThat(decoded).startsWith("1:testuser:");
    }

    @Test
    @DisplayName("login - should throw AuthenticationException when username not found")
    void login_usernameNotFound() {
        LoginRequest request = new LoginRequest("nonexistent", "password123");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    @DisplayName("login - should throw AuthenticationException when password is wrong")
    void login_wrongPassword() {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "$2a$10$encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");
    }

    // ========== GetUserById Tests ==========

    @Test
    @DisplayName("getUserById - should return UserDTO when user exists")
    void getUserById_success() {
        when(valueOperations.get("user:1")).thenReturn(null); // Cache miss
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDTO result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        
        verify(valueOperations).set(eq("user:1"), any(UserDTO.class), eq(15L), eq(java.util.concurrent.TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("getUserById - should throw ResourceNotFoundException when user not found")
    void getUserById_notFound() {
        when(valueOperations.get("user:999")).thenReturn(null); // Cache miss
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User");
    }
}
