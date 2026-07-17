package com.manager3d.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnalysisDataSourceRequest {
    @NotNull
    private Long dataSourceId;
    @NotBlank
    private String alias;
    private String tableName;
    private String querySql;
}
