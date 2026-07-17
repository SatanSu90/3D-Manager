package com.manager3d.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserStatusUpdateRequest {
    @NotBlank
    private String status;
}
