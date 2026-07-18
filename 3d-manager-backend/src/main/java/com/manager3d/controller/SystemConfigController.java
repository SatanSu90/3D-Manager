package com.manager3d.controller;

import com.manager3d.dto.request.SystemConfigUpdateRequest;
import com.manager3d.dto.response.SystemConfigResponse;
import com.manager3d.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemConfigController {

    private final SystemConfigService configService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll() {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", configService.getAllGroupedByCategory()
        ));
    }

    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", configService.getByKey(key)
        ));
    }

    @PutMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchUpdate(@RequestBody SystemConfigUpdateRequest request) {
        List<SystemConfigResponse> result = configService.batchUpdate(request);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "批量更新成功",
                "data", result
        ));
    }

    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String key,
            @RequestBody SystemConfigUpdateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新成功",
                "data", configService.update(key, request)
        ));
    }
}
