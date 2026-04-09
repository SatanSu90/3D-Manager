package com.manager3d.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ModelListItemResponse {
    private Long id;
    private String name;
    private String thumbnailUrl;
    private String format;
    private Long fileSize;
    private String categoryName;
    private List<String> tags;
    private Integer downloadCount;
    private LocalDateTime createdAt;
}
