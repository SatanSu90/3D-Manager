package com.manager3d.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnalysisTaskCreateRequest {
    @NotBlank
    private String name;
    private String description;
    private String config;
}
