package com.manager3d.controller;

import com.manager3d.dto.request.DepartmentCreateRequest;
import com.manager3d.dto.response.DepartmentResponse;
import com.manager3d.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDepartments() {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", departmentService.getAllDepartments()
        ));
    }

    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> getDepartmentTree() {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", departmentService.getDepartmentTree()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDepartment(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", departmentService.getDepartment(id)
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createDepartment(@RequestBody DepartmentCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "创建成功",
                "data", departmentService.createDepartment(request)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDepartment(
            @PathVariable Long id,
            @RequestBody DepartmentCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新成功",
                "data", departmentService.updateDepartment(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<Map<String, Object>> getDepartmentUsers(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", departmentService.getDepartmentUsers(id)
        ));
    }

    @PostMapping("/{id}/users")
    public ResponseEntity<Map<String, Object>> assignUsers(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> body) {
        List<Long> userIds = body.get("userIds");
        departmentService.assignUsers(id, userIds);
        return ResponseEntity.ok(Map.of("code", 200, "message", "分配成功"));
    }

    @DeleteMapping("/{id}/users/{userId}")
    public ResponseEntity<Map<String, Object>> removeUser(
            @PathVariable Long id,
            @PathVariable Long userId) {
        departmentService.removeUser(id, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "移除成功"));
    }
}
