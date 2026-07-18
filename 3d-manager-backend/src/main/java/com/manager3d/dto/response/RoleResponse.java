package com.manager3d.dto.response;

import com.manager3d.entity.Role;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
public class RoleResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private List<String> permissions;
    private Boolean isSystem;
    private Integer userCount;
    private LocalDateTime createdAt;

    public static RoleResponse from(Role role) {
        RoleResponse resp = new RoleResponse();
        resp.setId(role.getId());
        resp.setName(role.getName());
        resp.setCode(role.getCode());
        resp.setDescription(role.getDescription());
        resp.setIsSystem(role.getIsSystem());
        resp.setCreatedAt(role.getCreatedAt());
        resp.setPermissions(parsePermissions(role.getPermissions()));
        return resp;
    }

    public static RoleResponse fromWithUserCount(Role role, Integer userCount) {
        RoleResponse resp = from(role);
        resp.setUserCount(userCount);
        return resp;
    }

    private static List<String> parsePermissions(String permissions) {
        if (permissions == null || permissions.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = permissions.trim();
        if (trimmed.startsWith("[")) {
            // JSON array
            String inner = trimmed.substring(1, trimmed.length() - 1);
            if (inner.isBlank()) return Collections.emptyList();
            return Arrays.stream(inner.split(","))
                    .map(s -> s.replaceAll("^\"|\"$", "").trim())
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return Collections.singletonList(trimmed);
    }
}
