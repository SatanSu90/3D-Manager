package com.manager3d.controller;

import com.manager3d.dto.request.RoleCreateRequest;
import com.manager3d.dto.response.RoleResponse;
import com.manager3d.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRoles() {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", roleService.getAllRoles()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", roleService.getRole(id)
        ));
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<Map<String, Object>> getRoleUsers(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", roleService.getRoleUsers(id)
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRole(@RequestBody RoleCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "创建成功",
                "data", roleService.createRole(request)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRole(
            @PathVariable Long id,
            @RequestBody RoleCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新成功",
                "data", roleService.updateRole(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    @PostMapping("/{roleId}/users/{userId}")
    public ResponseEntity<Map<String, Object>> assignRole(
            @PathVariable Long roleId,
            @PathVariable Long userId) {
        roleService.assignRoleToUser(roleId, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "分配成功"));
    }

    @DeleteMapping("/{roleId}/users/{userId}")
    public ResponseEntity<Map<String, Object>> removeRole(
            @PathVariable Long roleId,
            @PathVariable Long userId) {
        roleService.removeRoleFromUser(roleId, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "移除成功"));
    }
}
