package com.manager3d.dto.response;

import com.manager3d.entity.Report;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportResponse {
    private Long id;
    private String name;
    private String description;
    private String type;
    private String config;
    private Long sceneId;
    private String status;
    private Long ownerId;
    private String ownerName;
    private String visibility;
    private String thumbnailKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReportResponse from(Report report) {
        ReportResponse resp = new ReportResponse();
        resp.setId(report.getId());
        resp.setName(report.getName());
        resp.setDescription(report.getDescription());
        resp.setType(report.getType() != null ? report.getType().name() : null);
        resp.setConfig(report.getConfig());
        resp.setSceneId(report.getSceneId());
        resp.setStatus(report.getStatus() != null ? report.getStatus().name() : null);
        resp.setOwnerId(report.getOwner() != null ? report.getOwner().getId() : null);
        resp.setOwnerName(report.getOwner() != null ? report.getOwner().getUsername() : null);
        resp.setVisibility(report.getVisibility() != null ? report.getVisibility().name() : null);
        resp.setThumbnailKey(report.getThumbnailKey());
        resp.setCreatedAt(report.getCreatedAt());
        resp.setUpdatedAt(report.getUpdatedAt());
        return resp;
    }
}
