package com.manager3d.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AnalysisResultResponse {
    /** 最终结果的列名列表 */
    private List<String> columns = new ArrayList<>();

    /** 最终结果的数据行 */
    private List<Map<String, Object>> rows = new ArrayList<>();

    /** 任务结果摘要 */
    private Map<String, Object> summary = new LinkedHashMap<>();

    /** 每个步骤的执行结果（按stepOrder排序） */
    private List<StepResult> stepResults = new ArrayList<>();

    /** 任务状态：SUCCESS / FAILED */
    private String status;

    /** 错误信息（失败时） */
    private String errorMessage;

    @Data
    public static class StepResult {
        private Long stepId;
        private Integer stepOrder;
        private String stepName;
        private String stepType;
        private String status;
        /** 步骤结果（统计数据/清洗后行数等） */
        private Object result;
        private String errorMessage;
    }
}
