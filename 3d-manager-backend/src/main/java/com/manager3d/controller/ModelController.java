package com.manager3d.controller;

import com.manager3d.entity.Model;
import com.manager3d.entity.User;
import com.manager3d.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelService modelService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getModels(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<Model> result = modelService.searchModels(keyword, categoryId, null, page, size, sortBy, sortDir);

        List<Map<String, Object>> content = result.getContent().stream()
                .map(this::toModelListItem)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", Map.of(
                        "content", content,
                        "totalElements", result.getTotalElements(),
                        "totalPages", result.getTotalPages(),
                        "number", result.getNumber(),
                        "size", result.getSize(),
                        "first", result.isFirst(),
                        "last", result.isLast()
                )
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getModel(@PathVariable Long id) {
        Model model = modelService.getModelById(id);
        return ResponseEntity.ok(Map.of("code", 200, "data", toModelDetail(model)));
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadModel(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) MultipartFile thumbnail,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        Model model = modelService.uploadModel(name, description, categoryId, tagIds, file, thumbnail, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "上传成功", "data", toModelDetail(model)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateModel(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Long categoryId = request.get("categoryId") != null ? Long.valueOf(request.get("categoryId").toString()) : null;

        @SuppressWarnings("unchecked")
        List<Long> tagIds = request.get("tagIds") != null
                ? ((List<Number>) request.get("tagIds")).stream().map(Number::longValue).collect(Collectors.toList())
                : null;

        Model model = modelService.updateModel(id, name, description, categoryId, tagIds, userId, isAdmin);
        return ResponseEntity.ok(Map.of("code", 200, "data", toModelDetail(model)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteModel(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        modelService.deleteModel(id, userId, isAdmin);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, Object>> downloadModel(@PathVariable Long id) {
        String url = modelService.getDownloadUrl(id);
        return ResponseEntity.ok(Map.of("code", 200, "data", Map.of("downloadUrl", url)));
    }

    private Map<String, Object> toModelListItem(Model m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("name", m.getName());
        map.put("thumbnailUrl", modelService.getThumbnailUrl(m));
        map.put("fileSize", m.getFileSize());
        map.put("format", m.getFormat());
        map.put("categoryId", m.getCategory() != null ? m.getCategory().getId() : null);
        map.put("categoryName", m.getCategory() != null ? m.getCategory().getName() : null);
        map.put("uploaderId", m.getUploader().getId());
        map.put("uploaderName", m.getUploader().getUsername());
        map.put("downloadCount", m.getDownloadCount());
        map.put("tags", m.getTags().stream().map(t -> Map.of("id", t.getId(), "name", t.getName())).collect(Collectors.toList()));
        map.put("createdAt", m.getCreatedAt());
        map.put("updatedAt", m.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toModelDetail(Model m) {
        Map<String, Object> map = toModelListItem(m);
        map.put("description", m.getDescription());
        map.put("fileKey", m.getFileKey());
        return map;
    }
}
