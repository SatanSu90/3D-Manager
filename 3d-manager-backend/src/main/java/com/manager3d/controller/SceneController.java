package com.manager3d.controller;

import com.manager3d.dto.request.SceneSaveRequest;
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
    public ResponseEntity<Map<String, Object>> getMyScenes(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", sceneService.getScenesByUsername(userDetails.getUsername())
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getScene(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", sceneService.getSceneById(id)
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
            @RequestBody SceneSaveRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", sceneService.updateScene(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteScene(@PathVariable Long id) {
        sceneService.deleteScene(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
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
