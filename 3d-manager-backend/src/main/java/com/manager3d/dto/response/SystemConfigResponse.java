package com.manager3d.dto.response;

import com.manager3d.entity.SystemConfig;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemConfigResponse {
    private Long id;
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private String category;
    private Boolean isEditable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SystemConfigResponse from(SystemConfig config) {
        SystemConfigResponse resp = new SystemConfigResponse();
        resp.setId(config.getId());
        resp.setConfigKey(config.getConfigKey());
        resp.setConfigValue(config.getConfigValue());
        resp.setConfigType(config.getConfigType().name());
        resp.setDescription(config.getDescription());
        resp.setCategory(config.getCategory());
        resp.setIsEditable(config.getIsEditable());
        resp.setCreatedAt(config.getCreatedAt());
        resp.setUpdatedAt(config.getUpdatedAt());
        return resp;
    }
}
