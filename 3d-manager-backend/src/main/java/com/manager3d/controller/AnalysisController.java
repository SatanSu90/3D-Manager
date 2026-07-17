package com.manager3d.controller;

import com.manager3d.dto.request.AnalysisDataSourceRequest;
import com.manager3d.dto.request.AnalysisStepCreateRequest;
import com.manager3d.dto.request.AnalysisTaskCreateRequest;
import com.manager3d.dto.request.IndicatorCreateRequest;
import com.manager3d.dto.response.AnalysisResultResponse;
import com.manager3d.dto.response.AnalysisTaskResponse;
import com.manager3d.dto.response.IndicatorResponse;
import com.manager3d.service.DataAnalysisService;
import com.manager3d.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final DataAnalysisService dataAnalysisService;
    private final UserService userService;

    // ===================== 任务管理 =====================

    @PostMapping("/tasks")
    public ResponseEntity<Map<String, Object>> createTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AnalysisTaskCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "创建成功",
                "data", dataAnalysisService.createTask(request, userDetails.getUsername())
        ));
    }

    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> listTasks(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long ownerId = isAdmin ? null : userService.getUserByUsername(userDetails.getUsername()).getId();
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", dataAnalysisService.listTasks(keyword, ownerId, page, size)
        ));
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", dataAnalysisService.getTaskDetail(id)
        ));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable Long id) {
        dataAnalysisService.deleteTask(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    // ===================== 数据源关联 =====================

    @PostMapping("/tasks/{id}/datasources")
    public ResponseEntity<Map<String, Object>> addDataSource(
            @PathVariable("id") Long taskId,
            @RequestBody AnalysisDataSourceRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "关联成功",
                "data", dataAnalysisService.addDataSource(taskId, request)
        ));
    }

    @DeleteMapping("/tasks/{id}/datasources/{dsId}")
    public ResponseEntity<Map<String, Object>> removeDataSource(
            @PathVariable("id") Long taskId,
            @PathVariable Long dsId) {
        dataAnalysisService.removeDataSource(taskId, dsId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "移除成功"));
    }

    // ===================== 步骤管理 =====================

    @PostMapping("/tasks/{id}/steps")
    public ResponseEntity<Map<String, Object>> addStep(
            @PathVariable("id") Long taskId,
            @RequestBody AnalysisStepCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "添加成功",
                "data", dataAnalysisService.addStep(taskId, request)
        ));
    }

    @PutMapping("/steps/{id}")
    public ResponseEntity<Map<String, Object>> updateStep(
            @PathVariable Long id,
            @RequestBody AnalysisStepCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新成功",
                "data", dataAnalysisService.updateStep(id, request)
        ));
    }

    @DeleteMapping("/steps/{id}")
    public ResponseEntity<Map<String, Object>> deleteStep(@PathVariable Long id) {
        dataAnalysisService.deleteStep(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    // ===================== 执行与结果 =====================

    @PostMapping("/tasks/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeTask(@PathVariable Long id) {
        AnalysisResultResponse result = dataAnalysisService.executeTask(id);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "执行完成",
                "data", result
        ));
    }

    @GetMapping("/tasks/{id}/results")
    public ResponseEntity<Map<String, Object>> getTaskResults(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", dataAnalysisService.getTaskResults(id)
        ));
    }

    @PostMapping("/tasks/{id}/save-indicator")
    public ResponseEntity<Map<String, Object>> saveAsIndicator(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody IndicatorCreateRequest request) {
        IndicatorResponse resp = dataAnalysisService.saveAsIndicator(id, request, userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "保存成功",
                "data", resp
        ));
    }
}
