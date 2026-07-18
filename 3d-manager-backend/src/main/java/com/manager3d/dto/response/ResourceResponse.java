package com.manager3d.dto.response;

import com.manager3d.entity.Resource;
import com.manager3d.service.MinioService;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceResponse {
    private Long id;
    private String name;
    private String type;
    private String fileKey;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private Integer width;
    private Integer height;
    private String tags;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ResourceResponse from(Resource resource, MinioService minioService) {
        ResourceResponse resp = new ResourceResponse();
        resp.setId(resource.getId());
        resp.setName(resource.getName());
        resp.setType(resource.getType() != null ? resource.getType().name() : null);
        resp.setFileKey(resource.getFileKey());
        if (resource.getFileKey() != null && minioService != null) {
            try {
                resp.setFileUrl(minioService.getResourceUrl(resource.getFileKey()));
            } catch (Exception e) {
                // MinIO不可用时跳过URL生成
            }
        }
        resp.setFileSize(resource.getFileSize());
        resp.setMimeType(resource.getMimeType());
        resp.setWidth(resource.getWidth());
        resp.setHeight(resource.getHeight());
        resp.setTags(resource.getTags());
        resp.setOwnerId(resource.getOwner() != null ? resource.getOwner().getId() : null);
        resp.setOwnerName(resource.getOwner() != null ? resource.getOwner().getUsername() : null);
        resp.setCreatedAt(resource.getCreatedAt());
        resp.setUpdatedAt(resource.getUpdatedAt());
        return resp;
    }
}
