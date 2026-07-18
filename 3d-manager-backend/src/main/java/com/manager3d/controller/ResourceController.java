package com.manager3d.controller;

import com.manager3d.dto.response.ResourceResponse;
import com.manager3d.service.ResourceService;
import com.manager3d.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listResources(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long ownerId = isAdmin ? null : userService.getUserByUsername(userDetails.getUsername()).getId();
        Page<ResourceResponse> data = resourceService.searchResources(keyword, type, ownerId, page, size);
        return ResponseEntity.ok(Map.of("code", 200, "data", data));
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResource(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "tags", required = false) String tags,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "上传成功",
                "data", resourceService.uploadResource(file, type, tags, userDetails.getUsername())
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchDelete(@RequestParam("ids") String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "批量删除完成",
                "data", resourceService.batchDelete(idList)
        ));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadResource(@PathVariable Long id) {
        ResourceService.ResourceDownloadResult result = resourceService.downloadResource(id);
        String encodedFilename = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .body(result.resource());
    }
}
