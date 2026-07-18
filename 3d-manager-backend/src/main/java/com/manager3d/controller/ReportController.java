package com.manager3d.controller;

import com.manager3d.dto.request.ReportCreateRequest;
import com.manager3d.dto.response.ReportResponse;
import com.manager3d.service.ReportService;
import com.manager3d.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listReports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long ownerId = isAdmin ? null : userService.getUserByUsername(userDetails.getUsername()).getId();
        Page<ReportResponse> data = reportService.searchReports(keyword, type, status, ownerId, page, size);
        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("code", 200, "data", reportService.getReport(id)));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ReportCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "创建成功",
                "data", reportService.createReport(request, userDetails.getUsername())
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateReport(
            @PathVariable Long id,
            @RequestBody ReportCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新成功",
                "data", reportService.updateReport(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteReport(@PathVariable Long id) {
        reportService.deleteReport(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    @PostMapping("/{id}/copy")
    public ResponseEntity<Map<String, Object>> copyReport(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "复制成功",
                "data", reportService.copyReport(id, userDetails.getUsername())
        ));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "状态更新成功",
                "data", reportService.updateReportStatus(id, body.get("status"))
        ));
    }

    @PostMapping("/from-scene/{sceneId}")
    public ResponseEntity<Map<String, Object>> generateFromScene(
            @PathVariable Long sceneId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "从场景生成报表成功",
                "data", reportService.generateReportFromScene(sceneId, userDetails.getUsername())
        ));
    }
}
