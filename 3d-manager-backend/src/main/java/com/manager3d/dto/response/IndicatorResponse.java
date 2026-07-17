package com.manager3d.dto.response;

import com.manager3d.entity.Indicator;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IndicatorResponse {
    private Long id;
    private String name;
    private String description;
    private String type;
    private String valueType;
    private String value;
    private Long dataSourceId;
    private String dataSourceName;
    private Long taskId;
    private Long ownerId;
    private String ownerName;
    private String tags;
    private String visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static IndicatorResponse from(Indicator indicator) {
        IndicatorResponse resp = new IndicatorResponse();
        resp.setId(indicator.getId());
        resp.setName(indicator.getName());
        resp.setDescription(indicator.getDescription());
        resp.setType(indicator.getType() != null ? indicator.getType().name() : null);
        resp.setValueType(indicator.getValueType() != null ? indicator.getValueType().name() : null);
        resp.setValue(indicator.getValue());
        resp.setDataSourceId(indicator.getDataSource() != null ? indicator.getDataSource().getId() : null);
        resp.setDataSourceName(indicator.getDataSource() != null ? indicator.getDataSource().getName() : null);
        resp.setTaskId(indicator.getTask() != null ? indicator.getTask().getId() : null);
        resp.setOwnerId(indicator.getOwner() != null ? indicator.getOwner().getId() : null);
        resp.setOwnerName(indicator.getOwner() != null ? indicator.getOwner().getUsername() : null);
        resp.setTags(indicator.getTags());
        resp.setVisibility(indicator.getVisibility() != null ? indicator.getVisibility().name() : null);
        resp.setCreatedAt(indicator.getCreatedAt());
        resp.setUpdatedAt(indicator.getUpdatedAt());
        return resp;
    }
}
