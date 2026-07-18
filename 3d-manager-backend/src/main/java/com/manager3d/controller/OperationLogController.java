package com.manager3d.controller;

import com.manager3d.dto.response.OperationLogResponse;
import com.manager3d.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/system/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class OperationLogController {

    private final OperationLogService logService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<OperationLogResponse> result = logService.list(module, action, userId, status,
                startDate, endDate, page, size);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", Map.of(
                        "items", result.getContent(),
                        "total", result.getTotalElements(),
                        "page", result.getNumber(),
                        "size", result.getSize(),
                        "totalPages", result.getTotalPages()
                )
        ));
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup(@RequestParam(defaultValue = "30") int days) {
        int deleted = logService.cleanup(days);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "清理成功",
                "data", Map.of("deleted", deleted, "days", days)
        ));
    }
}
