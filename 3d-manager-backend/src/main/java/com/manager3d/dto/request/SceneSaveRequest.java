package com.manager3d.dto.request;

import lombok.Data;

@Data
public class SceneSaveRequest {
    private String name;
    private String sceneData;
    private String thumbnailBase64;
}
