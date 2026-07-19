package com.manager3d.controller;

import com.manager3d.dto.request.SceneSaveRequest;
import com.manager3d.dto.request.ScenePreviewRequest;
import com.manager3d.dto.response.SceneResponse;
import com.manager3d.entity.Scene;
import com.manager3d.service.SceneService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scenes")
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getScenes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", sceneService.searchScenes(keyword, categoryId, status, userDetails.getUsername(), page, size)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getScene(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", sceneService.getSceneById(id)
        ));
    }

    @PostMapping("/{id}/preview")
    public ResponseEntity<Map<String, Object>> previewScene(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) ScenePreviewRequest request) {
        String password = request != null ? request.getPassword() : null;
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", sceneService.previewScene(id, userDetails.getUsername(), password)
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> saveScene(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SceneSaveRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", sceneService.saveScene(userDetails.getUsername(), request)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateScene(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SceneSaveRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", sceneService.updateScene(id, userDetails.getUsername(), request)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteScene(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        sceneService.deleteScene(id, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    @PostMapping("/{id}/copy")
    public ResponseEntity<Map<String, Object>> copyScene(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "复制成功",
                "data", sceneService.copyScene(id, userDetails.getUsername())
        ));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "状态更新成功",
                "data", sceneService.updateSceneStatus(id, userDetails.getUsername(), body.get("status"))
        ));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<Resource> exportScene(@PathVariable Long id) {
        Resource resource = sceneService.exportSceneAsGlb(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"scene_" + id + ".glb\"")
                .body(resource);
    }
}
