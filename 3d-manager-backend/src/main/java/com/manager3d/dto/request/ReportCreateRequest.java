package com.manager3d.dto.request;

import lombok.Data;

@Data
public class ReportCreateRequest {
    private String name;
    private String description;
    private String type;
    private String config;
    private Long sceneId;
    private String status;
    private String visibility;
    private String thumbnailKey;
}
