package com.manager3d.dto.request;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private String avatar;
    private String email;
    private String phone;
    private String oldPassword;
    private String newPassword;
}
