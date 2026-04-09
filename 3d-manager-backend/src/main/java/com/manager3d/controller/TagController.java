package com.manager3d.controller;

import com.manager3d.entity.Tag;
import com.manager3d.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTags() {
        return ResponseEntity.ok(Map.of("code", 200, "data", tagService.getAllTags()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createTag(@RequestBody TagCreateRequest request) {
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功", "data", tagService.createTag(request.getName())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateTag(@PathVariable Long id,
                                         @RequestBody TagCreateRequest request) {
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功", "data", tagService.updateTag(id, request.getName())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    public record TagCreateRequest(String name) {
        public String getName() {
            return name;
        }
    }
}
