package com.manager3d.dto.response;

import com.manager3d.entity.OperationLog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String targetType;
    private String targetId;
    private String description;
    private String requestUrl;
    private String requestMethod;
    private String ipAddress;
    private String status;
    private String errorMessage;
    private Integer durationMs;
    private LocalDateTime createdAt;

    public static OperationLogResponse from(OperationLog log) {
        OperationLogResponse resp = new OperationLogResponse();
        resp.setId(log.getId());
        resp.setUserId(log.getUserId());
        resp.setUsername(log.getUsername());
        resp.setModule(log.getModule());
        resp.setAction(log.getAction());
        resp.setTargetType(log.getTargetType());
        resp.setTargetId(log.getTargetId());
        resp.setDescription(log.getDescription());
        resp.setRequestUrl(log.getRequestUrl());
        resp.setRequestMethod(log.getRequestMethod());
        resp.setIpAddress(log.getIpAddress());
        resp.setStatus(log.getStatus().name());
        resp.setErrorMessage(log.getErrorMessage());
        resp.setDurationMs(log.getDurationMs());
        resp.setCreatedAt(log.getCreatedAt());
        return resp;
    }
}
