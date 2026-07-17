package com.manager3d.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnalysisStepCreateRequest {
    @NotBlank
    private String stepType;
    @NotBlank
    private String stepName;
    @NotBlank
    private String config;
    private Integer stepOrder;
}
