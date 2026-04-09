package com.manager3d.controller;

import com.manager3d.dto.request.CategoryCreateRequest;
import com.manager3d.entity.Category;
import com.manager3d.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        return ResponseEntity.ok(Map.of("code", 200, "data", categoryService.getAllCategories()));
    }

    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> getCategoryTree() {
        return ResponseEntity.ok(Map.of("code", 200, "data", categoryService.getCategoryTree()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("code", 200, "data", categoryService.getCategoryById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody CategoryCreateRequest request) {
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功", "data", categoryService.createCategory(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateCategory(@PathVariable Long id,
                                                   @RequestBody CategoryCreateRequest request) {
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功", "data", categoryService.updateCategory(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }
}
