package com.manager3d.controller;

import com.manager3d.dto.request.IndicatorCreateRequest;
import com.manager3d.dto.response.IndicatorResponse;
import com.manager3d.service.IndicatorService;
import com.manager3d.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/indicators")
@RequiredArgsConstructor
public class IndicatorController {

    private final IndicatorService indicatorService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listIndicators(
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
                "data", indicatorService.listIndicators(keyword, type, ownerId, page, size)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getIndicator(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "data", indicatorService.getIndicator(id)
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createIndicator(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody IndicatorCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "创建成功",
                "data", indicatorService.createIndicator(request, userDetails.getUsername())
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateIndicator(
            @PathVariable Long id,
            @RequestBody IndicatorCreateRequest request) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新成功",
                "data", indicatorService.updateIndicator(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteIndicator(@PathVariable Long id) {
        indicatorService.deleteIndicator(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }
}
