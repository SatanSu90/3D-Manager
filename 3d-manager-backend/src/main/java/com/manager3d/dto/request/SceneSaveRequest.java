package com.manager3d.dto.request;

import lombok.Data;

@Data
public class SceneSaveRequest {
    private String name;
    private String description;
    private String sceneData;
    private String thumbnailBase64;
    private Long categoryId;
    private String visibility;
    private String status;
    private String resolution;
    private String previewPassword;
}
