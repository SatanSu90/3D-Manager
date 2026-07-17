package com.manager3d.dto.response;

import com.manager3d.entity.Scene;
import com.manager3d.service.MinioService;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SceneResponse {
    private Long id;
    private String name;
    private String description;
    private String sceneData;
    private String thumbnailUrl;
    private String creatorName;
    private Long creatorId;
    private Long categoryId;
    private String categoryName;
    private Long ownerId;
    private String ownerName;
    private String visibility;
    private String status;
    private String resolution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SceneResponse fromScene(Scene scene, MinioService minioService) {
        SceneResponse response = new SceneResponse();
        response.setId(scene.getId());
        response.setName(scene.getName());
        response.setDescription(scene.getDescription());
        response.setSceneData(scene.getSceneData());
        if (scene.getThumbnailKey() != null) {
            response.setThumbnailUrl(minioService.getThumbnailUrl(scene.getThumbnailKey()));
        }
        response.setCreatorName(scene.getCreator().getUsername());
        response.setCreatorId(scene.getCreator().getId());
        response.setCategoryId(scene.getCategory() != null ? scene.getCategory().getId() : null);
        response.setCategoryName(scene.getCategory() != null ? scene.getCategory().getName() : null);
        response.setOwnerId(scene.getOwner() != null ? scene.getOwner().getId() : null);
        response.setOwnerName(scene.getOwner() != null ? scene.getOwner().getUsername() : null);
        response.setVisibility(scene.getVisibility() != null ? scene.getVisibility().name() : "PRIVATE");
        response.setStatus(scene.getStatus() != null ? scene.getStatus().name() : "DRAFT");
        response.setResolution(scene.getResolution());
        response.setCreatedAt(scene.getCreatedAt());
        response.setUpdatedAt(scene.getUpdatedAt());
        return response;
    }
}
