package com.manager3d.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DataSourceTestRequest {
    @NotBlank
    private String type;
    @NotBlank
    private String host;
    @NotBlank
    private Integer port;
    private String databaseName;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    private Boolean sslEnabled;
}
