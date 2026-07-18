package com.manager3d.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class SystemConfigUpdateRequest {

    private String configValue;

    private String configType;

    private String description;

    private String category;

    private Boolean isEditable;

    private Map<String, String> items;
}
