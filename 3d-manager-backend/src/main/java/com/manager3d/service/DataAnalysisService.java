package com.manager3d.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manager3d.dto.request.AnalysisDataSourceRequest;
import com.manager3d.dto.request.AnalysisStepCreateRequest;
import com.manager3d.dto.request.AnalysisTaskCreateRequest;
import com.manager3d.dto.request.IndicatorCreateRequest;
import com.manager3d.dto.response.AnalysisResultResponse;
import com.manager3d.dto.response.AnalysisTaskResponse;
import com.manager3d.dto.response.DataPreviewResponse;
import com.manager3d.dto.response.IndicatorResponse;
import com.manager3d.entity.*;
import com.manager3d.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataAnalysisService {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final AnalysisStepRepository analysisStepRepository;
    private final AnalysisDataSourceRepository analysisDataSourceRepository;
    private final IndicatorRepository indicatorRepository;
    private final DataSourceRepository dataSourceRepository;
    private final UserRepository userRepository;
    private final DataSourceService dataSourceService;
    private final ObjectMapper objectMapper;

    // ===================== 任务管理 =====================

    @Transactional
    public AnalysisTaskResponse createTask(AnalysisTaskCreateRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        AnalysisTask task = AnalysisTask.builder()
                .name(request.getName())
                .description(request.getDescription())
                .config(request.getConfig())
                .status(AnalysisTask.Status.DRAFT)
                .owner(owner)
                .build();

        task = analysisTaskRepository.save(task);
        return AnalysisTaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public AnalysisTaskResponse getTaskDetail(Long id) {
        AnalysisTask task = analysisTaskRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("分析任务不存在"));
        return AnalysisTaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public Page<AnalysisTaskResponse> listTasks(String keyword, Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AnalysisTask> taskPage = analysisTaskRepository.searchTasks(keyword, ownerId, pageable);
        return taskPage.map(t -> AnalysisTaskResponse.from(t, false, false));
    }

    @Transactional
    public void deleteTask(Long id) {
        if (!analysisTaskRepository.existsById(id)) {
            throw new RuntimeException("分析任务不存在");
        }
        analysisTaskRepository.deleteById(id);
    }

    // ===================== 数据源关联 =====================

    @Transactional
    public AnalysisTaskResponse addDataSource(Long taskId, AnalysisDataSourceRequest request) {
        AnalysisTask task = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("分析任务不存在"));
        DataSource ds = dataSourceRepository.findById(request.getDataSourceId())
                .orElseThrow(() -> new RuntimeException("数据源不存在"));

        if (request.getTableName() == null && request.getQuerySql() == null) {
            throw new RuntimeException("必须指定 tableName 或 querySql");
        }

        AnalysisDataSource ads = AnalysisDataSource.builder()
                .task(task)
                .dataSource(ds)
                .alias(request.getAlias())
                .tableName(request.getTableName())
                .querySql(request.getQuerySql())
                .build();
        task.getAnalysisDataSources().add(ads);
        analysisTaskRepository.save(task);
        return AnalysisTaskResponse.from(analysisTaskRepository.findByIdWithDetails(taskId)
                .orElse(task));
    }

    @Transactional
    public void removeDataSource(Long taskId, Long dsId) {
        AnalysisTask task = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("分析任务不存在"));
        AnalysisDataSource toRemove = task.getAnalysisDataSources().stream()
                .filter(a -> a.getId().equals(dsId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("任务未关联该数据源"));
        task.getAnalysisDataSources().remove(toRemove);
        analysisTaskRepository.save(task);
    }

    // ===================== 步骤管理 =====================

    @Transactional
    public AnalysisTaskResponse addStep(Long taskId, AnalysisStepCreateRequest request) {
        AnalysisTask task = analysisTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("分析任务不存在"));

        int order = request.getStepOrder() != null ? request.getStepOrder()
                : (task.getAnalysisSteps().stream()
                        .mapToInt(AnalysisStep::getStepOrder).max().orElse(0) + 1);

        AnalysisStep step = AnalysisStep.builder()
                .task(task)
                .stepOrder(order)
                .stepType(AnalysisStep.StepType.valueOf(request.getStepType()))
                .stepName(request.getStepName())
                .config(request.getConfig())
                .status(AnalysisStep.Status.PENDING)
                .build();
        task.getAnalysisSteps().add(step);
        analysisTaskRepository.save(task);
        return AnalysisTaskResponse.from(analysisTaskRepository.findByIdWithDetails(taskId)
                .orElse(task));
    }

    @Transactional
    public AnalysisTaskResponse updateStep(Long stepId, AnalysisStepCreateRequest request) {
        AnalysisStep step = analysisStepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("分析步骤不存在"));
        if (request.getStepType() != null) {
            step.setStepType(AnalysisStep.StepType.valueOf(request.getStepType()));
        }
        if (request.getStepName() != null) step.setStepName(request.getStepName());
        if (request.getConfig() != null) step.setConfig(request.getConfig());
        if (request.getStepOrder() != null) step.setStepOrder(request.getStepOrder());
        analysisStepRepository.save(step);
        return AnalysisTaskResponse.from(step.getTask());
    }

    @Transactional
    public void deleteStep(Long stepId) {
        if (!analysisStepRepository.existsById(stepId)) {
            throw new RuntimeException("分析步骤不存在");
        }
        analysisStepRepository.deleteById(stepId);
    }

    // ===================== 执行引擎 =====================

    @Transactional
    public AnalysisResultResponse executeTask(Long taskId) {
        AnalysisTask task = analysisTaskRepository.findByIdWithDetails(taskId)
                .orElseThrow(() -> new RuntimeException("分析任务不存在"));

        // 标记为运行中
        task.setStatus(AnalysisTask.Status.RUNNING);
        task.setErrorMessage(null);
        analysisTaskRepository.save(task);

        AnalysisResultResponse response = new AnalysisResultResponse();
        response.setStatus(AnalysisTask.Status.RUNNING.name());

        try {
            // 1. 加载数据源数据
            if (task.getAnalysisDataSources() == null || task.getAnalysisDataSources().isEmpty()) {
                throw new RuntimeException("任务未关联任何数据源");
            }
            AnalysisDataSource primaryDs = task.getAnalysisDataSources().get(0);
            List<Map<String, Object>> currentRows = loadDataFromDataSource(primaryDs);
            int loadedRowCount = currentRows.size();

            // 2. 按 step_order 顺序执行步骤
            List<AnalysisStep> steps = task.getAnalysisSteps().stream()
                    .sorted(Comparator.comparingInt(AnalysisStep::getStepOrder))
                    .collect(Collectors.toList());

            for (AnalysisStep step : steps) {
                AnalysisResultResponse.StepResult sr = new AnalysisResultResponse.StepResult();
                sr.setStepId(step.getId());
                sr.setStepOrder(step.getStepOrder());
                sr.setStepName(step.getStepName());
                sr.setStepType(step.getStepType() != null ? step.getStepType().name() : null);

                try {
                    Object stepResult = executeStep(step, currentRows);
                    String resultJson = objectMapper.writeValueAsString(stepResult);
                    step.setResult(resultJson);
                    step.setStatus(AnalysisStep.Status.SUCCESS);
                    sr.setStatus(AnalysisStep.Status.SUCCESS.name());
                    sr.setResult(stepResult);

                    // 仅 CLEAN / FILTER 会改变行数据
                    if (step.getStepType() == AnalysisStep.StepType.CLEAN
                            || step.getStepType() == AnalysisStep.StepType.FILTER) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> transformed = (List<Map<String, Object>>) stepResult;
                        currentRows = transformed;
                    }
                } catch (Exception e) {
                    step.setStatus(AnalysisStep.Status.FAILED);
                    step.setResult(null);
                    sr.setStatus(AnalysisStep.Status.FAILED.name());
                    sr.setErrorMessage(e.getMessage());
                    analysisStepRepository.save(step);
                    response.getStepResults().add(sr);
                    throw e;
                }
                analysisStepRepository.save(step);
                response.getStepResults().add(sr);
            }

            // 3. 构建最终结果
            response.setColumns(extractColumns(currentRows));
            response.setRows(currentRows);
            response.getSummary().put("totalRows", currentRows.size());
            response.getSummary().put("stepCount", steps.size());
            response.getSummary().put("dataRowsLoaded", loadedRowCount);

            task.setStatus(AnalysisTask.Status.SUCCESS);
            task.setExecutedAt(LocalDateTime.now());
            task.setResultSummary(objectMapper.writeValueAsString(response.getSummary()));
            task.setErrorMessage(null);
            analysisTaskRepository.save(task);

            response.setStatus(AnalysisTask.Status.SUCCESS.name());
            return response;

        } catch (Exception e) {
            log.error("分析任务执行失败: taskId={}, error={}", taskId, e.getMessage());
            task.setStatus(AnalysisTask.Status.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setExecutedAt(LocalDateTime.now());
            analysisTaskRepository.save(task);
            response.setStatus(AnalysisTask.Status.FAILED.name());
            response.setErrorMessage(e.getMessage());
            return response;
        }
    }

    @Transactional(readOnly = true)
    public AnalysisResultResponse getTaskResults(Long taskId) {
        AnalysisTask task = analysisTaskRepository.findByIdWithDetails(taskId)
                .orElseThrow(() -> new RuntimeException("分析任务不存在"));

        AnalysisResultResponse response = new AnalysisResultResponse();
        response.setStatus(task.getStatus() != null ? task.getStatus().name() : null);
        response.setErrorMessage(task.getErrorMessage());

        if (task.getResultSummary() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> summary = objectMapper.readValue(task.getResultSummary(), Map.class);
                response.setSummary(summary);
            } catch (Exception ignored) { }
        }

        if (task.getAnalysisSteps() != null) {
            List<AnalysisStep> sorted = task.getAnalysisSteps().stream()
                    .sorted(Comparator.comparingInt(AnalysisStep::getStepOrder))
                    .collect(Collectors.toList());
            for (AnalysisStep step : sorted) {
                AnalysisResultResponse.StepResult sr = new AnalysisResultResponse.StepResult();
                sr.setStepId(step.getId());
                sr.setStepOrder(step.getStepOrder());
                sr.setStepName(step.getStepName());
                sr.setStepType(step.getStepType() != null ? step.getStepType().name() : null);
                sr.setStatus(step.getStatus() != null ? step.getStatus().name() : null);
                if (step.getResult() != null) {
                    try {
                        sr.setResult(objectMapper.readValue(step.getResult(), Object.class));
                    } catch (Exception ignored) { }
                }
                response.getStepResults().add(sr);
            }
        }
        return response;
    }

    /**
     * 根据步骤类型分发到对应的分析方法。
     */
    private Object executeStep(AnalysisStep step, List<Map<String, Object>> inputData) throws Exception {
        JsonNode config = objectMapper.readTree(step.getConfig() != null ? step.getConfig() : "{}");
        return switch (step.getStepType()) {
            case CLEAN -> cleanData(inputData, config);
            case FILTER -> filterData(inputData, config);
            case STATS -> basicStats(inputData, config);
            case ADVANCED_STATS -> advancedStats(inputData, config);
            case ML -> mlAnalysis(inputData, config);
        };
    }

    // ===================== 数据加载 =====================

    /**
     * 复用 DataSourceService 的 JDBC 查询能力加载数据。
     * 优先使用 querySql；否则使用 tableName 构造 SELECT 语句。
     */
    private List<Map<String, Object>> loadDataFromDataSource(AnalysisDataSource ads) {
        if (ads.getDataSource() == null) {
            throw new RuntimeException("关联的数据源不存在");
        }
        Long dsId = ads.getDataSource().getId();
        DataPreviewResponse resp;
        if (ads.getQuerySql() != null && !ads.getQuerySql().isBlank()) {
            resp = dataSourceService.executeQuery(dsId, ads.getQuerySql());
        } else if (ads.getTableName() != null && !ads.getTableName().isBlank()) {
            resp = dataSourceService.executeQuery(dsId, "SELECT * FROM " + ads.getTableName());
        } else {
            throw new RuntimeException("数据源关联必须指定 tableName 或 querySql");
        }
        return resp.getRows() != null ? resp.getRows() : new ArrayList<>();
    }

    // ===================== 数据清洗 =====================

    /**
     * 清洗数据。配置示例：
     * {
     *   "typeConversion": [{"field":"age","type":"INTEGER"}],
     *   "removeNull": true,
     *   "nullStrategy": "DROP_ROW",
     *   "fillValues": {"name":"N/A"},
     *   "dedup": true,
     *   "dedupFields": ["id"],
     *   "outlierRemoval": {"fields":["price"],"factor":1.5}
     * }
     */
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> cleanData(List<Map<String, Object>> rows, JsonNode config) {
        List<Map<String, Object>> result = new ArrayList<>(rows);

        // 1. 类型转换
        if (config.has("typeConversion") && config.get("typeConversion").isArray()) {
            for (JsonNode conv : config.get("typeConversion")) {
                String field = conv.get("field").asText();
                String type = conv.get("type").asText();
                for (Map<String, Object> row : result) {
                    if (row.containsKey(field)) {
                        row.put(field, convertValue(row.get(field), type));
                    }
                }
            }
        }

        // 2. 空值处理
        if (config.has("removeNull") && config.get("removeNull").asBoolean()) {
            String strategy = config.has("nullStrategy") ? config.get("nullStrategy").asText() : "DROP_ROW";
            Map<String, Object> fillValues = new HashMap<>();
            if (config.has("fillValues") && config.get("fillValues").isObject()) {
                fillValues = objectMapper.convertValue(config.get("fillValues"), Map.class);
            }
            List<Map<String, Object>> nonNullRows = new ArrayList<>();
            for (Map<String, Object> row : result) {
                boolean hasNull = row.values().stream().anyMatch(Objects::isNull);
                if (hasNull) {
                    if ("DROP_ROW".equals(strategy)) {
                        continue;
                    } else if ("FILL_DEFAULT".equals(strategy)) {
                        Map<String, Object> filled = new LinkedHashMap<>(row);
                        for (Map.Entry<String, Object> e : filled.entrySet()) {
                            if (e.getValue() == null && fillValues.containsKey(e.getKey())) {
                                e.setValue(fillValues.get(e.getKey()));
                            }
                        }
                        nonNullRows.add(filled);
                    } else {
                        nonNullRows.add(row);
                    }
                } else {
                    nonNullRows.add(row);
                }
            }
            result = nonNullRows;
        }

        // 3. 去重
        if (config.has("dedup") && config.get("dedup").asBoolean()) {
            List<String> dedupFields = new ArrayList<>();
            if (config.has("dedupFields") && config.get("dedupFields").isArray()) {
                for (JsonNode f : config.get("dedupFields")) {
                    dedupFields.add(f.asText());
                }
            }
            Set<String> seen = new HashSet<>();
            List<Map<String, Object>> uniqueRows = new ArrayList<>();
            for (Map<String, Object> row : result) {
                String key;
                if (dedupFields.isEmpty()) {
                    key = row.toString();
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (String f : dedupFields) {
                        sb.append(row.get(f)).append("|");
                    }
                    key = sb.toString();
                }
                if (seen.add(key)) {
                    uniqueRows.add(row);
                }
            }
            result = uniqueRows;
        }

        // 4. 离群值剔除 (IQR)
        if (config.has("outlierRemoval") && config.get("outlierRemoval").isObject()) {
            JsonNode or = config.get("outlierRemoval");
            List<String> fields = new ArrayList<>();
            if (or.has("fields") && or.get("fields").isArray()) {
                for (JsonNode f : or.get("fields")) fields.add(f.asText());
            }
            double factor = or.has("factor") ? or.get("factor").asDouble() : 1.5;
            for (String field : fields) {
                double[] values = result.stream()
                        .map(r -> r.get(field))
                        .filter(Objects::nonNull)
                        .filter(v -> v instanceof Number || isNumericString(v))
                        .mapToDouble(this::toDouble)
                        .sorted()
                        .toArray();
                if (values.length < 4) continue;
                DescriptiveStatistics stats = new DescriptiveStatistics(values);
                double q1 = stats.getPercentile(25);
                double q3 = stats.getPercentile(75);
                double iqr = q3 - q1;
                double lower = q1 - factor * iqr;
                double upper = q3 + factor * iqr;
                result = result.stream().filter(r -> {
                    Object v = r.get(field);
                    if (v == null) return true;
                    try {
                        double d = toDouble(v);
                        return d >= lower && d <= upper;
                    } catch (Exception e) {
                        return true;
                    }
                }).collect(Collectors.toList());
            }
        }

        return result;
    }

    // ===================== 数据过滤 =====================

    /**
     * 过滤数据。配置示例：
     * {
     *   "conditions": [{"field":"age","operator":"GTE","value":18}],
     *   "logic": "AND",
     *   "selectFields": ["name","age"],
     *   "orderBy": {"field":"age","direction":"DESC"},
     *   "limit": 100
     * }
     */
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> filterData(List<Map<String, Object>> rows, JsonNode config) {
        // 1. 条件过滤
        List<Map<String, Object>> filtered = new ArrayList<>(rows);
        if (config.has("conditions") && config.get("conditions").isArray()
                && config.get("conditions").size() > 0) {
            String logic = config.has("logic") ? config.get("logic").asText() : "AND";
            List<JsonNode> conds = new ArrayList<>();
            for (JsonNode c : config.get("conditions")) conds.add(c);
            filtered = filtered.stream()
                    .filter(row -> {
                        boolean result;
                        if ("OR".equalsIgnoreCase(logic)) {
                            result = conds.stream().anyMatch(c -> matchesCondition(row, c));
                        } else {
                            result = conds.stream().allMatch(c -> matchesCondition(row, c));
                        }
                        return result;
                    })
                    .collect(Collectors.toList());
        }

        // 2. 字段选择
        if (config.has("selectFields") && config.get("selectFields").isArray()
                && config.get("selectFields").size() > 0) {
            List<String> fields = new ArrayList<>();
            for (JsonNode f : config.get("selectFields")) fields.add(f.asText());
            filtered = filtered.stream().map(row -> {
                Map<String, Object> projected = new LinkedHashMap<>();
                for (String f : fields) {
                    projected.put(f, row.get(f));
                }
                return projected;
            }).collect(Collectors.toList());
        }

        // 3. 排序
        if (config.has("orderBy") && config.get("orderBy").isObject()) {
            JsonNode ob = config.get("orderBy");
            String field = ob.get("field").asText();
            boolean asc = !ob.has("direction") || "ASC".equalsIgnoreCase(ob.get("direction").asText());
            filtered.sort((a, b) -> {
                Object va = a.get(field);
                Object vb = b.get(field);
                int cmp = compareValues(va, vb);
                return asc ? cmp : -cmp;
            });
        }

        // 4. 限制行数
        if (config.has("limit") && config.get("limit").isInt()) {
            int limit = config.get("limit").asInt();
            if (filtered.size() > limit) {
                filtered = new ArrayList<>(filtered.subList(0, limit));
            }
        }

        return filtered;
    }

    private boolean matchesCondition(Map<String, Object> row, JsonNode cond) {
        String field = cond.get("field").asText();
        String operator = cond.has("operator") ? cond.get("operator").asText() : "EQ";
        JsonNode valueNode = cond.has("value") ? cond.get("value") : null;
        Object fieldValue = row.get(field);

        switch (operator) {
            case "IS_NULL":
                return fieldValue == null;
            case "NOT_NULL":
                return fieldValue != null;
            case "EQ":
                return Objects.equals(fieldValue, jsonToObject(valueNode));
            case "NEQ":
                return !Objects.equals(fieldValue, jsonToObject(valueNode));
            case "GT":
                return compareValues(fieldValue, jsonToObject(valueNode)) > 0;
            case "GTE":
                return compareValues(fieldValue, jsonToObject(valueNode)) >= 0;
            case "LT":
                return compareValues(fieldValue, jsonToObject(valueNode)) < 0;
            case "LTE":
                return compareValues(fieldValue, jsonToObject(valueNode)) <= 0;
            case "LIKE":
                return fieldValue != null && likeMatch(fieldValue.toString(), valueNode.asText());
            case "IN":
                if (valueNode == null || !valueNode.isArray()) return false;
                for (JsonNode v : valueNode) {
                    if (Objects.equals(fieldValue, jsonToObject(v))) return true;
                }
                return false;
            case "BETWEEN":
                if (valueNode == null || !valueNode.isArray() || valueNode.size() < 2) return false;
                try {
                    double dv = toDouble(fieldValue);
                    return dv >= valueNode.get(0).asDouble() && dv <= valueNode.get(1).asDouble();
                } catch (Exception e) {
                    return false;
                }
            default:
                throw new RuntimeException("不支持的操作符: " + operator);
        }
    }

    private boolean likeMatch(String value, String pattern) {
        StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '%' -> regex.append(".*");
                case '_' -> regex.append(".");
                default -> {
                    if ("\\.[]{}()*+-?^$|".indexOf(c) >= 0) regex.append("\\");
                    regex.append(c);
                }
            }
        }
        return Pattern.matches(regex.toString(), value);
    }

    // ===================== 基础统计 =====================

    /**
     * 基础统计。配置示例：
     * {"fields":["age","salary"]}
     * 返回: {"age":{"COUNT":100,"MEAN":35.5,"MEDIAN":34.0,...}, ...}
     */
    Map<String, Object> basicStats(List<Map<String, Object>> rows, JsonNode config) {
        List<String> fields = new ArrayList<>();
        if (config.has("fields") && config.get("fields").isArray()) {
            for (JsonNode f : config.get("fields")) fields.add(f.asText());
        }
        if (fields.isEmpty()) {
            // 默认取所有数值字段
            if (!rows.isEmpty()) {
                for (String key : rows.get(0).keySet()) {
                    if (rows.stream().anyMatch(r -> r.get(key) instanceof Number)) {
                        fields.add(key);
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (String field : fields) {
            double[] values = rows.stream()
                    .map(r -> r.get(field))
                    .filter(Objects::nonNull)
                    .filter(v -> v instanceof Number || isNumericString(v))
                    .mapToDouble(this::toDouble)
                    .toArray();

            Map<String, Object> fieldStats = new LinkedHashMap<>();
            if (values.length == 0) {
                fieldStats.put("COUNT", 0);
                result.put(field, fieldStats);
                continue;
            }
            DescriptiveStatistics stats = new DescriptiveStatistics(values);
            fieldStats.put("COUNT", stats.getN());
            fieldStats.put("MEAN", stats.getMean());
            fieldStats.put("MEDIAN", stats.getPercentile(50));
            fieldStats.put("STD_DEV", stats.getStandardDeviation());
            fieldStats.put("MIN", stats.getMin());
            fieldStats.put("MAX", stats.getMax());
            fieldStats.put("SUM", stats.getSum());
            fieldStats.put("Q1", stats.getPercentile(25));
            fieldStats.put("Q3", stats.getPercentile(75));
            result.put(field, fieldStats);
        }
        return result;
    }

    // ===================== 高级统计 =====================

    /**
     * 高级统计。配置示例：
     * CORRELATION: {"type":"CORRELATION","fields":["x","y"]}
     * GROUP_BY:    {"type":"GROUP_BY","groupBy":"dept","aggregates":[{"field":"salary","func":"AVG"}]}
     * CROSS_TAB:   {"type":"CROSS_TAB","rowField":"dept","colField":"gender","valueField":"salary","aggFunc":"AVG"}
     */
    Map<String, Object> advancedStats(List<Map<String, Object>> rows, JsonNode config) {
        String type = config.has("type") ? config.get("type").asText() : "CORRELATION";
        return switch (type) {
            case "CORRELATION" -> correlation(rows, config);
            case "GROUP_BY" -> groupBy(rows, config);
            case "CROSS_TAB" -> crossTab(rows, config);
            default -> throw new RuntimeException("不支持的高级统计类型: " + type);
        };
    }

    private Map<String, Object> correlation(List<Map<String, Object>> rows, JsonNode config) {
        List<String> fields = new ArrayList<>();
        if (config.has("fields") && config.get("fields").isArray()) {
            for (JsonNode f : config.get("fields")) fields.add(f.asText());
        }
        if (fields.size() < 2) {
            throw new RuntimeException("CORRELATION 需要至少两个字段");
        }

        // 提取每个字段的数值数组
        Map<String, double[]> values = new LinkedHashMap<>();
        for (String field : fields) {
            values.put(field, rows.stream()
                    .map(r -> r.get(field))
                    .filter(Objects::nonNull)
                    .filter(v -> v instanceof Number || isNumericString(v))
                    .mapToDouble(this::toDouble)
                    .toArray());
        }

        // 构建相关系数矩阵
        double[][] matrix = new double[fields.size()][fields.size()];
        PearsonsCorrelation pc = new PearsonsCorrelation();
        for (int i = 0; i < fields.size(); i++) {
            for (int j = 0; j < fields.size(); j++) {
                if (i == j) {
                    matrix[i][j] = 1.0;
                } else {
                    double[] xi = values.get(fields.get(i));
                    double[] xj = values.get(fields.get(j));
                    int len = Math.min(xi.length, xj.length);
                    if (len < 2) {
                        matrix[i][j] = Double.NaN;
                    } else {
                        double[] a = Arrays.copyOf(xi, len);
                        double[] b = Arrays.copyOf(xj, len);
                        try {
                            matrix[i][j] = pc.correlation(a, b);
                        } catch (Exception e) {
                            matrix[i][j] = Double.NaN;
                        }
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fields", fields);
        result.put("matrix", matrix);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> groupBy(List<Map<String, Object>> rows, JsonNode config) {
        String groupByField = config.get("groupBy").asText();
        List<String> aggFields = new ArrayList<>();
        Map<String, String> aggFuncs = new LinkedHashMap<>();
        if (config.has("aggregates") && config.get("aggregates").isArray()) {
            for (JsonNode a : config.get("aggregates")) {
                String f = a.get("field").asText();
                String func = a.has("func") ? a.get("func").asText() : "COUNT";
                aggFields.add(f);
                aggFuncs.put(f, func);
            }
        }

        // 分组
        Map<String, List<Map<String, Object>>> groups = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> {
                            Object v = r.get(groupByField);
                            return v == null ? "null" : v.toString();
                        },
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<Map<String, Object>> resultRows = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : groups.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(groupByField, entry.getKey());
            List<Map<String, Object>> groupRows = entry.getValue();
            row.put("_count", groupRows.size());
            for (String field : aggFields) {
                String func = aggFuncs.get(field);
                row.put(field + "_" + func, aggregate(groupRows, field, func));
            }
            resultRows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupBy", groupByField);
        result.put("rows", resultRows);
        return result;
    }

    private Map<String, Object> crossTab(List<Map<String, Object>> rows, JsonNode config) {
        String rowField = config.get("rowField").asText();
        String colField = config.get("colField").asText();
        String valueField = config.has("valueField") ? config.get("valueField").asText() : null;
        String aggFunc = config.has("aggFunc") ? config.get("aggFunc").asText() : "COUNT";

        // 收集所有行/列值（保持顺序）
        List<String> rowValues = new ArrayList<>();
        List<String> colValues = new ArrayList<>();
        Set<String> rowSet = new LinkedHashSet<>();
        Set<String> colSet = new LinkedHashSet<>();
        for (Map<String, Object> r : rows) {
            Object rv = r.get(rowField);
            Object cv = r.get(colField);
            String rs = rv == null ? "null" : rv.toString();
            String cs = cv == null ? "null" : cv.toString();
            if (rowSet.add(rs)) rowValues.add(rs);
            if (colSet.add(cs)) colValues.add(cs);
        }

        // 构建分组：rowKey -> colKey -> values
        Map<String, Map<String, List<Double>>> grid = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            Object rv = r.get(rowField);
            Object cv = r.get(colField);
            String rs = rv == null ? "null" : rv.toString();
            String cs = cv == null ? "null" : cv.toString();
            grid.computeIfAbsent(rs, k -> new LinkedHashMap<>())
                    .computeIfAbsent(cs, k -> new ArrayList<>());
            if (valueField != null) {
                Object v = r.get(valueField);
                if (v != null && (v instanceof Number || isNumericString(v))) {
                    grid.get(rs).get(cs).add(toDouble(v));
                }
            }
        }

        // 构建值矩阵
        double[][] values = new double[rowValues.size()][colValues.size()];
        for (int i = 0; i < rowValues.size(); i++) {
            for (int j = 0; j < colValues.size(); j++) {
                List<Double> cell = grid.getOrDefault(rowValues.get(i), Collections.emptyMap())
                        .getOrDefault(colValues.get(j), Collections.emptyList());
                values[i][j] = aggregateDoubles(cell, aggFunc);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rowField", rowField);
        result.put("colField", colField);
        result.put("valueField", valueField);
        result.put("aggFunc", aggFunc);
        result.put("rows", rowValues);
        result.put("columns", colValues);
        result.put("values", values);
        return result;
    }

    private double aggregateDoubles(List<Double> values, String func) {
        if (values.isEmpty()) return 0.0;
        DescriptiveStatistics stats = new DescriptiveStatistics();
        values.forEach(stats::addValue);
        return switch (func) {
            case "SUM" -> stats.getSum();
            case "AVG" -> stats.getMean();
            case "MIN" -> stats.getMin();
            case "MAX" -> stats.getMax();
            case "COUNT" -> stats.getN();
            case "MEDIAN" -> stats.getPercentile(50);
            default -> stats.getN();
        };
    }

    private Object aggregate(List<Map<String, Object>> rows, String field, String func) {
        if ("COUNT".equals(func)) {
            return rows.size();
        }
        double[] values = rows.stream()
                .map(r -> r.get(field))
                .filter(Objects::nonNull)
                .filter(v -> v instanceof Number || isNumericString(v))
                .mapToDouble(this::toDouble)
                .toArray();
        if (values.length == 0) return 0;
        DescriptiveStatistics stats = new DescriptiveStatistics(values);
        return switch (func) {
            case "SUM" -> stats.getSum();
            case "AVG" -> stats.getMean();
            case "MIN" -> stats.getMin();
            case "MAX" -> stats.getMax();
            case "MEDIAN" -> stats.getPercentile(50);
            default -> stats.getN();
        };
    }

    // ===================== 机器学习 =====================

    /**
     * 机器学习分析入口。配置示例：
     * LINEAR_REGRESSION: {"algorithm":"LINEAR_REGRESSION","features":["age","exp"],"target":"salary"}
     * KMEANS:            {"algorithm":"KMEANS","fields":["salary","age"],"k":3,"maxIterations":100}
     * DECISION_TREE:     {"algorithm":"DECISION_TREE","features":["age","salary"],"target":"dept"}
     */
    Map<String, Object> mlAnalysis(List<Map<String, Object>> rows, JsonNode config) {
        String algorithm = config.has("algorithm") ? config.get("algorithm").asText() : "";
        return switch (algorithm) {
            case "LINEAR_REGRESSION" -> linearRegression(rows, config);
            case "KMEANS" -> kmeansCluster(rows, config);
            case "DECISION_TREE" -> decisionTree(rows, config);
            default -> throw new RuntimeException("不支持的机器学习算法: " + algorithm);
        };
    }

    /**
     * 线性回归（最小二乘法）。返回截距、各特征系数、R²、样本量。
     */
    private Map<String, Object> linearRegression(List<Map<String, Object>> rows, JsonNode config) {
        List<String> features = extractStringList(config, "features");
        String target = config.has("target") ? config.get("target").asText() : null;
        if (features.isEmpty() || target == null) {
            throw new RuntimeException("LINEAR_REGRESSION 需要 features 和 target 配置");
        }
        if (rows.size() < features.size() + 1) {
            throw new RuntimeException("样本量不足，至少需要 " + (features.size() + 1) + " 条数据");
        }

        // 构建数据矩阵
        double[][] X = new double[rows.size()][features.size()];
        double[] y = new double[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            for (int j = 0; j < features.size(); j++) {
                X[i][j] = toDoubleSafe(rows.get(i).get(features.get(j)));
            }
            y[i] = toDoubleSafe(rows.get(i).get(target));
        }

        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.newSampleData(y, X);

        double[] beta = regression.estimateRegressionParameters();
        double rSquared = regression.calculateRSquared();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algorithm", "LINEAR_REGRESSION");
        result.put("intercept", beta[0]);
        List<Map<String, Object>> coefficients = new ArrayList<>();
        for (int j = 0; j < features.size(); j++) {
            Map<String, Object> coef = new LinkedHashMap<>();
            coef.put("feature", features.get(j));
            coef.put("coefficient", beta[j + 1]);
            coefficients.add(coef);
        }
        result.put("coefficients", coefficients);
        result.put("rSquared", rSquared);
        result.put("sampleSize", rows.size());
        return result;
    }

    /**
     * K-Means++ 聚类。返回各簇中心、大小、成员索引（样本量<=1000时）。
     */
    private Map<String, Object> kmeansCluster(List<Map<String, Object>> rows, JsonNode config) {
        List<String> fields = extractStringList(config, "fields");
        int k = config.has("k") ? config.get("k").asInt() : 3;
        int maxIter = config.has("maxIterations") ? config.get("maxIterations").asInt() : 100;

        if (fields.isEmpty()) {
            throw new RuntimeException("KMEANS 需要 fields 配置");
        }
        if (rows.size() < k) {
            throw new RuntimeException("样本量 " + rows.size() + " 小于聚类数 k=" + k);
        }

        List<ClusterPoint> points = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            double[] val = new double[fields.size()];
            for (int j = 0; j < fields.size(); j++) {
                val[j] = toDoubleSafe(rows.get(i).get(fields.get(j)));
            }
            points.add(new ClusterPoint(val, i));
        }

        KMeansPlusPlusClusterer<ClusterPoint> clusterer = new KMeansPlusPlusClusterer<>(k, maxIter);
        List<CentroidCluster<ClusterPoint>> clusters = clusterer.cluster(points);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algorithm", "KMEANS");
        result.put("k", k);
        result.put("sampleSize", rows.size());

        List<Map<String, Object>> clusterResults = new ArrayList<>();
        for (int c = 0; c < clusters.size(); c++) {
            CentroidCluster<ClusterPoint> cluster = clusters.get(c);
            Map<String, Object> clusterInfo = new LinkedHashMap<>();
            clusterInfo.put("clusterId", c + 1);
            clusterInfo.put("size", cluster.getPoints().size());

            double[] center = cluster.getCenter().getPoint();
            List<Map<String, Object>> centerInfo = new ArrayList<>();
            for (int j = 0; j < fields.size(); j++) {
                Map<String, Object> cInfo = new LinkedHashMap<>();
                cInfo.put("field", fields.get(j));
                cInfo.put("center", center[j]);
                centerInfo.add(cInfo);
            }
            clusterInfo.put("center", centerInfo);

            if (rows.size() <= 1000) {
                List<Integer> memberIndices = cluster.getPoints().stream()
                        .map(p -> p.rowIndex)
                        .collect(Collectors.toList());
                clusterInfo.put("members", memberIndices);
            }
            clusterResults.add(clusterInfo);
        }
        result.put("clusters", clusterResults);
        return result;
    }

    /**
     * 决策树分类（C4.5 风格，基于信息增益，假设特征值为离散字符串）。
     */
    private Map<String, Object> decisionTree(List<Map<String, Object>> rows, JsonNode config) {
        List<String> features = extractStringList(config, "features");
        String target = config.has("target") ? config.get("target").asText() : null;
        if (features.isEmpty() || target == null) {
            throw new RuntimeException("DECISION_TREE 需要 features 和 target 配置");
        }
        if (rows.isEmpty()) {
            throw new RuntimeException("DECISION_TREE 需要至少一条数据");
        }

        TreeNode tree = buildTree(rows, features, target, 0, 5);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("algorithm", "DECISION_TREE");
        result.put("sampleSize", rows.size());
        result.put("tree", serializeTreeNode(tree));

        int correct = 0;
        for (Map<String, Object> row : rows) {
            String predicted = classify(tree, row);
            String actual = String.valueOf(row.get(target));
            if (predicted.equals(actual)) correct++;
        }
        result.put("accuracy", (double) correct / rows.size());
        return result;
    }

    /**
     * 递归构建决策树。
     */
    private TreeNode buildTree(List<Map<String, Object>> rows, List<String> features,
                               String target, int depth, int maxDepth) {
        TreeNode node = new TreeNode();

        // 终止条件1：所有样本同类别
        Set<String> labels = rows.stream()
                .map(r -> String.valueOf(r.get(target)))
                .collect(Collectors.toSet());
        if (labels.size() == 1) {
            node.isLeaf = true;
            node.label = labels.iterator().next();
            return node;
        }

        // 终止条件2：达到最大深度或无特征可用
        if (depth >= maxDepth || features.isEmpty()) {
            node.isLeaf = true;
            node.label = majorityLabel(rows, target);
            return node;
        }

        // 选择最佳分裂特征（信息增益最大）
        String bestFeature = null;
        double bestGain = -1;
        for (String f : features) {
            double gain = calculateInformationGain(rows, f, target);
            if (gain > bestGain) {
                bestGain = gain;
                bestFeature = f;
            }
        }

        // 如果没有任何信息增益，作为叶子节点
        if (bestFeature == null || bestGain <= 0) {
            node.isLeaf = true;
            node.label = majorityLabel(rows, target);
            return node;
        }

        node.feature = bestFeature;
        node.isLeaf = false;
        node.children = new LinkedHashMap<>();

        // 按 bestFeature 的值划分子集，递归构建
        Map<String, List<Map<String, Object>>> partitions = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String key = String.valueOf(row.get(bestFeature));
            partitions.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        List<String> remainingFeatures = new ArrayList<>(features);
        remainingFeatures.remove(bestFeature);

        for (Map.Entry<String, List<Map<String, Object>>> e : partitions.entrySet()) {
            node.children.put(e.getKey(),
                    buildTree(e.getValue(), remainingFeatures, target, depth + 1, maxDepth));
        }
        return node;
    }

    /**
     * 计算熵。
     */
    private double calculateEntropy(List<Map<String, Object>> rows, String target) {
        if (rows.isEmpty()) return 0.0;
        Map<String, Long> counts = rows.stream()
                .map(r -> String.valueOf(r.get(target)))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        double entropy = 0.0;
        double total = rows.size();
        for (long c : counts.values()) {
            double p = c / total;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    /**
     * 计算信息增益。
     */
    private double calculateInformationGain(List<Map<String, Object>> rows,
                                            String feature, String target) {
        double baseEntropy = calculateEntropy(rows, target);
        Map<String, List<Map<String, Object>>> partitions = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String key = String.valueOf(row.get(feature));
            partitions.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        double newEntropy = 0.0;
        double total = rows.size();
        for (List<Map<String, Object>> subset : partitions.values()) {
            newEntropy += (subset.size() / total) * calculateEntropy(subset, target);
        }
        return baseEntropy - newEntropy;
    }

    /**
     * 取多数类别标签。
     */
    private String majorityLabel(List<Map<String, Object>> rows, String target) {
        Map<String, Long> counts = rows.stream()
                .map(r -> String.valueOf(r.get(target)))
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    /**
     * 使用决策树对单行数据进行分类。
     */
    private String classify(TreeNode node, Map<String, Object> row) {
        while (!node.isLeaf && node.feature != null) {
            String key = String.valueOf(row.get(node.feature));
            TreeNode child = node.children.get(key);
            if (child == null) {
                // 未见过的特征值，回退到该节点下多数标签（即第一个子节点的多数标签近似）
                return majorityLabelAtNode(node, row);
            }
            node = child;
        }
        return node.label;
    }

    /**
     * 节点中无法匹配子节点时的回退策略：返回子节点中样本量最多的标签。
     */
    private String majorityLabelAtNode(TreeNode node, Map<String, Object> row) {
        if (node.isLeaf) return node.label;
        if (node.children == null || node.children.isEmpty()) return node.label;
        // 简化：直接返回第一个子树的标签
        for (TreeNode child : node.children.values()) {
            return classify(child, row);
        }
        return node.label;
    }

    /**
     * 序列化决策树节点为 Map 结构（便于 JSON 输出）。
     */
    private Map<String, Object> serializeTreeNode(TreeNode node) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("isLeaf", node.isLeaf);
        if (node.isLeaf) {
            m.put("label", node.label);
        } else {
            m.put("feature", node.feature);
            Map<String, Object> children = new LinkedHashMap<>();
            if (node.children != null) {
                for (Map.Entry<String, TreeNode> e : node.children.entrySet()) {
                    children.put(e.getKey(), serializeTreeNode(e.getValue()));
                }
            }
            m.put("children", children);
        }
        return m;
    }

    /**
     * K-Means 数据点包装类。
     */
    private static class ClusterPoint implements Clusterable {
        private final double[] point;
        final int rowIndex;

        ClusterPoint(double[] point, int rowIndex) {
            this.point = point;
            this.rowIndex = rowIndex;
        }

        @Override
        public double[] getPoint() {
            return point;
        }
    }

    /**
     * 决策树节点。
     */
    private static class TreeNode {
        String feature;
        String label;
        Map<String, TreeNode> children;
        boolean isLeaf;
    }

    // ===================== 保存为指标 =====================

    @Transactional
    public IndicatorResponse saveAsIndicator(Long taskId, IndicatorCreateRequest request, String username) {
        AnalysisTask task = analysisTaskRepository.findByIdWithDetails(taskId)
                .orElseThrow(() -> new RuntimeException("分析任务不存在"));
        if (task.getStatus() != AnalysisTask.Status.SUCCESS) {
            throw new RuntimeException("任务未成功执行，无法保存为指标");
        }
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        AnalysisResultResponse results = getTaskResults(taskId);
        Indicator.ValueType valueType = request.getValueType() != null
                ? Indicator.ValueType.valueOf(request.getValueType())
                : Indicator.ValueType.JSON;

        String value;
        if (results.getRows() != null && !results.getRows().isEmpty()) {
            Map<String, Object> valueMap = new LinkedHashMap<>();
            valueMap.put("columns", results.getColumns());
            valueMap.put("rows", results.getRows());
            valueMap.put("summary", results.getSummary());
            try {
                value = objectMapper.writeValueAsString(valueMap);
            } catch (Exception e) {
                throw new RuntimeException("序列化结果失败: " + e.getMessage());
            }
        } else if (task.getResultSummary() != null) {
            value = task.getResultSummary();
        } else {
            value = "{}";
        }

        Indicator indicator = Indicator.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(Indicator.Type.DERIVED)
                .valueType(valueType)
                .value(value)
                .task(task)
                .owner(owner)
                .tags(request.getTags())
                .visibility(request.getVisibilityEnum())
                .build();

        indicator = indicatorRepository.save(indicator);
        return IndicatorResponse.from(indicator);
    }

    // ===================== 辅助方法 =====================

    private List<String> extractColumns(List<Map<String, Object>> rows) {
        LinkedHashSet<String> cols = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            cols.addAll(row.keySet());
        }
        return new ArrayList<>(cols);
    }

    private Object convertValue(Object value, String type) {
        if (value == null) return null;
        try {
            return switch (type.toUpperCase()) {
                case "INTEGER", "INT" -> {
                    if (value instanceof Number n) yield n.intValue();
                    yield Integer.parseInt(value.toString().trim());
                }
                case "LONG" -> {
                    if (value instanceof Number n) yield n.longValue();
                    yield Long.parseLong(value.toString().trim());
                }
                case "DOUBLE", "FLOAT" -> {
                    if (value instanceof Number n) yield n.doubleValue();
                    yield Double.parseDouble(value.toString().trim());
                }
                case "STRING" -> value.toString();
                case "BOOLEAN" -> {
                    if (value instanceof Boolean b) yield b;
                    String s = value.toString().trim().toLowerCase();
                    yield "true".equals(s) || "1".equals(s) || "yes".equals(s);
                }
                default -> value;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private Object jsonToObject(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble()) return node.asDouble();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isTextual()) return node.asText();
        return node.toString();
    }

    private int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Number || b instanceof Number
                || isNumericString(a) || isNumericString(b)) {
            try {
                return Double.compare(toDouble(a), toDouble(b));
            } catch (Exception e) {
                // fall through to string compare
            }
        }
        return a.toString().compareTo(b.toString());
    }

    private boolean isNumericString(Object v) {
        if (v == null) return false;
        if (v instanceof Number) return true;
        String s = v.toString().trim();
        if (s.isEmpty()) return false;
        try {
            Double.parseDouble(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private double toDouble(Object v) {
        if (v == null) throw new RuntimeException("值为空");
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(v.toString().trim());
    }

    /**
     * ML 场景使用的容错版本：null 或无法解析时返回 0，避免整体崩溃。
     */
    private double toDoubleSafe(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 从 config 中提取字符串列表（如 features / fields）。
     */
    private List<String> extractStringList(JsonNode config, String key) {
        List<String> result = new ArrayList<>();
        if (config.has(key) && config.get(key).isArray()) {
            config.get(key).forEach(n -> result.add(n.asText()));
        }
        return result;
    }
}
