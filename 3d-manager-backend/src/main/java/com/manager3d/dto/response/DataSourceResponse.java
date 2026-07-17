package com.manager3d.dto.response;

import com.manager3d.entity.DataSource;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DataSourceResponse {
    private Long id;
    private String name;
    private String type;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private Boolean sslEnabled;
    private String status;
    private Long ownerId;
    private String ownerName;
    private String visibility;
    private LocalDateTime lastTestTime;
    private Boolean lastTestResult;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DataSourceResponse from(DataSource ds) {
        DataSourceResponse resp = new DataSourceResponse();
        resp.setId(ds.getId());
        resp.setName(ds.getName());
        resp.setType(ds.getType() != null ? ds.getType().name() : null);
        resp.setHost(ds.getHost());
        resp.setPort(ds.getPort());
        resp.setDatabaseName(ds.getDatabaseName());
        resp.setUsername(ds.getUsername());
        resp.setSslEnabled(ds.getSslEnabled());
        resp.setStatus(ds.getStatus() != null ? ds.getStatus().name() : null);
        resp.setOwnerId(ds.getOwner() != null ? ds.getOwner().getId() : null);
        resp.setOwnerName(ds.getOwner() != null ? ds.getOwner().getUsername() : null);
        resp.setVisibility(ds.getVisibility() != null ? ds.getVisibility().name() : null);
        resp.setLastTestTime(ds.getLastTestTime());
        resp.setLastTestResult(ds.getLastTestResult());
        resp.setCreatedAt(ds.getCreatedAt());
        resp.setUpdatedAt(ds.getUpdatedAt());
        return resp;
    }
}
