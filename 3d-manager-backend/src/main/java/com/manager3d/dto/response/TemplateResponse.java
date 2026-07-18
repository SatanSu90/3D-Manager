package com.manager3d.dto.response;

import com.manager3d.entity.Template;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TemplateResponse {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String config;
    private String previewImage;
    private Boolean isOfficial;
    private Integer useCount;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TemplateResponse from(Template template) {
        TemplateResponse resp = new TemplateResponse();
        resp.setId(template.getId());
        resp.setName(template.getName());
        resp.setDescription(template.getDescription());
        resp.setCategory(template.getCategory() != null ? template.getCategory().name() : null);
        resp.setConfig(template.getConfig());
        resp.setPreviewImage(template.getPreviewImage());
        resp.setIsOfficial(template.getIsOfficial());
        resp.setUseCount(template.getUseCount());
        resp.setOwnerId(template.getOwner() != null ? template.getOwner().getId() : null);
        resp.setOwnerName(template.getOwner() != null ? template.getOwner().getUsername() : null);
        resp.setCreatedAt(template.getCreatedAt());
        resp.setUpdatedAt(template.getUpdatedAt());
        return resp;
    }
}
