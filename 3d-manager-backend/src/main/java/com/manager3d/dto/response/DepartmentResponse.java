package com.manager3d.dto.response;

import com.manager3d.entity.Department;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DepartmentResponse {
    private Long id;
    private String name;
    private Long parentId;
    private String parentName;
    private Integer sortOrder;
    private Integer userCount;
    private LocalDateTime createdAt;
    private List<DepartmentResponse> children = new ArrayList<>();

    public static DepartmentResponse from(Department dept) {
        DepartmentResponse resp = new DepartmentResponse();
        resp.setId(dept.getId());
        resp.setName(dept.getName());
        resp.setParentId(dept.getParent() != null ? dept.getParent().getId() : null);
        resp.setParentName(dept.getParent() != null ? dept.getParent().getName() : null);
        resp.setSortOrder(dept.getSortOrder());
        resp.setUserCount(dept.getUsers() != null ? dept.getUsers().size() : 0);
        resp.setCreatedAt(dept.getCreatedAt());
        return resp;
    }

    public static DepartmentResponse fromWithChildren(Department dept) {
        DepartmentResponse resp = from(dept);
        if (dept.getChildren() != null) {
            for (Department child : dept.getChildren()) {
                resp.getChildren().add(fromWithChildren(child));
            }
        }
        return resp;
    }
}
