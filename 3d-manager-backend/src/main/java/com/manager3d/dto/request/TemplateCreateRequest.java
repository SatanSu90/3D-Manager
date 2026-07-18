package com.manager3d.dto.request;

import lombok.Data;

@Data
public class TemplateCreateRequest {
    private String name;
    private String description;
    private String category;
    private String config;
    private String previewImage;
    private Boolean isOfficial;
}
