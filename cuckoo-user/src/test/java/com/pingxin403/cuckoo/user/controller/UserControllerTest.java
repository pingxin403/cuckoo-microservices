package com.pingxin403.cuckoo.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingxin403.cuckoo.common.exception.AuthenticationException;
import com.pingxin403.cuckoo.common.exception.DuplicateResourceException;
import com.pingxin403.cuckoo.common.exception.GlobalExceptionHandler;
import com.pingxin403.cuckoo.common.exception.ResourceNotFoundException;
import com.pingxin403.cuckoo.user.dto.*;
import com.pingxin403.cuckoo.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 单元测试（Standalone MockMvc）
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        // Register Jackson JavaTimeModule for LocalDateTime serialization
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ========== Register Tests ==========

    @Test
    @DisplayName("POST /api/users/register - should return 201 with UserDTO")
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");
        UserDTO userDTO = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.register(any(RegisterRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/users/register - should return 409 when username exists")
    void register_duplicateUsername() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("User", "username", "testuser"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("POST /api/users/register - should return 409 when email exists")
    void register_duplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123");

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("User", "email", "test@example.com"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // ========== Login Tests ==========

    @Test
    @DisplayName("POST /api/users/login - should return 200 with token and userId")
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "password123");
        LoginResponse response = LoginResponse.builder()
                .token("dGVzdHRva2Vu")
                .userId(1L)
                .build();

        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("dGVzdHRva2Vu"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @DisplayName("POST /api/users/login - should return 401 when credentials are invalid")
    void login_invalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");

        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // ========== GetUserById Tests ==========

    @Test
    @DisplayName("GET /api/users/{id} - should return 200 with UserDTO")
    void getUserById_success() throws Exception {
        UserDTO userDTO = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userService.getUserById(eq(1L))).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/users/{id} - should return 404 when user not found")
    void getUserById_notFound() throws Exception {
        when(userService.getUserById(eq(999L)))
                .thenThrow(new ResourceNotFoundException("User", 999L));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
