package com.manager3d.controller;

import com.manager3d.dto.request.UserProfileUpdateRequest;
import com.manager3d.dto.request.UserStatusUpdateRequest;
import com.manager3d.entity.User;
import com.manager3d.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        return ResponseEntity.ok(Map.of("code", 200, "data", userService.getAllUsers()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("code", 200, "data", userService.getUserById(id)));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProfile(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("code", 200, "data", user));
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMyProfile(
            Authentication authentication,
            @RequestBody UserProfileUpdateRequest request) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.getUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新成功",
                "data", userService.updateProfile(user.getId(), request)
        ));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateRole(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        String role = body.get("role");
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功", "data", userService.updateUserRole(id, role)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody UserStatusUpdateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "状态更新成功",
                "data", userService.updateStatus(id, request.getStatus())
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }
}
