package com.manager3d.controller;

import com.manager3d.dto.request.TemplateCreateRequest;
import com.manager3d.dto.response.TemplateResponse;
import com.manager3d.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listTemplates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TemplateResponse> data = templateService.searchTemplates(keyword, category, page, size);
        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("code", 200, "data", templateService.getTemplate(id)));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTemplate(
            @RequestBody TemplateCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "创建成功",
                "data", templateService.createTemplate(request, userDetails.getUsername())
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateTemplate(
            @PathVariable Long id,
            @RequestBody TemplateCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新成功",
                "data", templateService.updateTemplate(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<Map<String, Object>> applyTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "应用模板成功",
                "data", templateService.applyTemplate(id)
        ));
    }
}
