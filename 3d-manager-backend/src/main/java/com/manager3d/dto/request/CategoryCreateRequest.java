package com.manager3d.dto.request;

import lombok.Data;

@Data
public class CategoryCreateRequest {
    private String name;
    private Long parentId;
    private Integer sortOrder;
}
