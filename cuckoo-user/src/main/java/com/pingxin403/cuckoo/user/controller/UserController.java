package com.pingxin403.cuckoo.user.controller;

import com.pingxin403.cuckoo.common.controller.BaseController;
import com.pingxin403.cuckoo.user.dto.*;
import com.pingxin403.cuckoo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 提供用户注册、登录和查询 REST API
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController extends BaseController {

    private final UserService userService;

    /**
     * 用户注册
     * POST /api/users/register
     */
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody RegisterRequest request) {
        logRequest("用户注册", request.getUsername(), request.getEmail());
        UserDTO user = userService.register(request);
        logResponse("用户注册", user.getId());
        return created(user);
    }

    /**
     * 用户登录
     * POST /api/users/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        logRequest("用户登录", request.getUsername());
        LoginResponse response = userService.login(request);
        logResponse("用户登录", response.getUserId());
        return ok(response);
    }

    /**
     * 根据 ID 查询用户
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        logRequest("查询用户", id);
        UserDTO user = userService.getUserById(id);
        logResponse("查询用户", user.getId());
        return ok(user);
    }
}
