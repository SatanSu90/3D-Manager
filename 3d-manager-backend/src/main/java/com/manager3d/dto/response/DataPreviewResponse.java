package com.manager3d.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataPreviewResponse {
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private int total;
}
