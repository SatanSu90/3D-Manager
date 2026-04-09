package com.manager3d.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ModelResponse {
    private Long id;
    private String name;
    private String description;
    private String fileUrl;
    private String thumbnailUrl;
    private Long fileSize;
    private String format;
    private String categoryName;
    private Long categoryId;
    private String uploaderName;
    private Long uploaderId;
    private Integer downloadCount;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
