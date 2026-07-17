package com.manager3d.dto.response;

import com.manager3d.entity.AnalysisDataSource;
import com.manager3d.entity.AnalysisStep;
import com.manager3d.entity.AnalysisTask;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class AnalysisTaskResponse {
    private Long id;
    private String name;
    private String description;
    private String status;
    private Long ownerId;
    private String ownerName;
    private String config;
    private String resultSummary;
    private String errorMessage;
    private LocalDateTime executedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StepInfo> steps = new ArrayList<>();
    private List<DataSourceInfo> dataSources = new ArrayList<>();

    public static AnalysisTaskResponse from(AnalysisTask task) {
        return from(task, true, true);
    }

    public static AnalysisTaskResponse from(AnalysisTask task, boolean includeSteps, boolean includeDataSources) {
        AnalysisTaskResponse resp = new AnalysisTaskResponse();
        resp.setId(task.getId());
        resp.setName(task.getName());
        resp.setDescription(task.getDescription());
        resp.setStatus(task.getStatus() != null ? task.getStatus().name() : null);
        resp.setOwnerId(task.getOwner() != null ? task.getOwner().getId() : null);
        resp.setOwnerName(task.getOwner() != null ? task.getOwner().getUsername() : null);
        resp.setConfig(task.getConfig());
        resp.setResultSummary(task.getResultSummary());
        resp.setErrorMessage(task.getErrorMessage());
        resp.setExecutedAt(task.getExecutedAt());
        resp.setCreatedAt(task.getCreatedAt());
        resp.setUpdatedAt(task.getUpdatedAt());

        if (includeSteps && task.getAnalysisSteps() != null) {
            resp.setSteps(task.getAnalysisSteps().stream()
                    .map(StepInfo::from)
                    .collect(Collectors.toList()));
        }

        if (includeDataSources && task.getAnalysisDataSources() != null) {
            resp.setDataSources(task.getAnalysisDataSources().stream()
                    .map(DataSourceInfo::from)
                    .collect(Collectors.toList()));
        }
        return resp;
    }

    @Data
    public static class StepInfo {
        private Long id;
        private Integer stepOrder;
        private String stepType;
        private String stepName;
        private String config;
        private String result;
        private String status;

        public static StepInfo from(AnalysisStep step) {
            StepInfo info = new StepInfo();
            info.setId(step.getId());
            info.setStepOrder(step.getStepOrder());
            info.setStepType(step.getStepType() != null ? step.getStepType().name() : null);
            info.setStepName(step.getStepName());
            info.setConfig(step.getConfig());
            info.setResult(step.getResult());
            info.setStatus(step.getStatus() != null ? step.getStatus().name() : null);
            return info;
        }
    }

    @Data
    public static class DataSourceInfo {
        private Long id;
        private Long dataSourceId;
        private String dataSourceName;
        private String alias;
        private String tableName;
        private String querySql;

        public static DataSourceInfo from(AnalysisDataSource ads) {
            DataSourceInfo info = new DataSourceInfo();
            info.setId(ads.getId());
            info.setDataSourceId(ads.getDataSource() != null ? ads.getDataSource().getId() : null);
            info.setDataSourceName(ads.getDataSource() != null ? ads.getDataSource().getName() : null);
            info.setAlias(ads.getAlias());
            info.setTableName(ads.getTableName());
            info.setQuerySql(ads.getQuerySql());
            return info;
        }
    }

    /**
     * 转换为简单的Map表示，方便在结果响应中使用。
     */
    public Map<String, Object> toMap() {
        return Map.of(
                "id", id,
                "name", name,
                "status", status,
                "steps", steps,
                "dataSources", dataSources
        );
    }
}
