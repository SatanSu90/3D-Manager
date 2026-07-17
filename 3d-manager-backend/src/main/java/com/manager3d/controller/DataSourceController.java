package com.manager3d.controller;

import com.manager3d.dto.request.DataSourceCreateRequest;
import com.manager3d.dto.request.DataSourceTestRequest;
import com.manager3d.dto.response.DataPreviewResponse;
import com.manager3d.dto.response.DataSourceResponse;
import com.manager3d.service.DataSourceService;
import com.manager3d.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDataSources(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long ownerId = isAdmin ? null : userService.getUserByUsername(userDetails.getUsername()).getId();
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", dataSourceService.getDataSources(keyword, type, ownerId, page, size)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDataSource(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", dataSourceService.getDataSource(id)
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createDataSource(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody DataSourceCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "创建成功",
                "data", dataSourceService.createDataSource(request, userDetails.getUsername())
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDataSource(
            @PathVariable Long id,
            @RequestBody DataSourceCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新成功",
                "data", dataSourceService.updateDataSource(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDataSource(@PathVariable Long id) {
        dataSourceService.deleteDataSource(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnectionDirect(@RequestBody DataSourceTestRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", dataSourceService.testConnectionDirect(request)
        ));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", dataSourceService.testConnection(id)
        ));
    }

    @GetMapping("/{id}/tables")
    public ResponseEntity<Map<String, Object>> getTables(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", dataSourceService.getTables(id)
        ));
    }

    @GetMapping("/{id}/tables/{tableName}/columns")
    public ResponseEntity<Map<String, Object>> getColumns(
            @PathVariable Long id,
            @PathVariable String tableName) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", dataSourceService.getColumns(id, tableName)
        ));
    }

    @GetMapping("/{id}/tables/{tableName}/preview")
    public ResponseEntity<Map<String, Object>> previewData(
            @PathVariable Long id,
            @PathVariable String tableName,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", dataSourceService.previewData(id, tableName, limit)
        ));
    }

    @PostMapping("/{id}/query")
    public ResponseEntity<Map<String, Object>> executeQuery(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", dataSourceService.executeQuery(id, sql)
        ));
    }
}
