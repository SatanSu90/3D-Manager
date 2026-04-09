package com.manager3d.dto.response;

import com.manager3d.entity.Scene;
import com.manager3d.service.MinioService;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SceneResponse {
    private Long id;
    private String name;
    private String sceneData;
    private String thumbnailUrl;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SceneResponse fromScene(Scene scene, MinioService minioService) {
        SceneResponse response = new SceneResponse();
        response.setId(scene.getId());
        response.setName(scene.getName());
        response.setSceneData(scene.getSceneData());
        if (scene.getThumbnailKey() != null) {
            response.setThumbnailUrl(minioService.getThumbnailUrl(scene.getThumbnailKey()));
        }
        response.setCreatorName(scene.getCreator().getUsername());
        response.setCreatedAt(scene.getCreatedAt());
        response.setUpdatedAt(scene.getUpdatedAt());
        return response;
    }
}
