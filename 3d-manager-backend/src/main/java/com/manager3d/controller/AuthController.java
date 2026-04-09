package com.manager3d.controller;

import com.manager3d.entity.User;
import com.manager3d.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "登录成功",
                "data", authService.login(request.get("username"), request.get("password"))
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "注册成功",
                "data", authService.register(request.get("username"), request.get("password"))
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "刷新成功",
                "data", authService.refresh(request.get("refreshToken"))
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        User user = authService.getCurrentUser(userId);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "role", user.getRole().name(),
                        "avatar", user.getAvatar() != null ? user.getAvatar() : ""
                )
        ));
    }
}
