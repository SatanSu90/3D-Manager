package com.manager3d.service;

import com.manager3d.dto.request.SystemConfigUpdateRequest;
import com.manager3d.dto.response.SystemConfigResponse;
import com.manager3d.entity.SystemConfig;
import com.manager3d.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository configRepository;

    @Transactional(readOnly = true)
    public Map<String, List<SystemConfigResponse>> getAllGroupedByCategory() {
        List<SystemConfig> configs = configRepository.findAll();
        Map<String, List<SystemConfig>> grouped = configs.stream()
                .collect(Collectors.groupingBy(c -> c.getCategory() != null ? c.getCategory() : "general",
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, List<SystemConfigResponse>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<SystemConfig>> entry : grouped.entrySet()) {
            result.put(entry.getKey(),
                    entry.getValue().stream().map(SystemConfigResponse::from).collect(Collectors.toList()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public SystemConfigResponse getByKey(String key) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new RuntimeException("配置项不存在: " + key));
        return SystemConfigResponse.from(config);
    }

    @Transactional(readOnly = true)
    public String getStringValue(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    @Transactional
    public SystemConfigResponse update(String key, SystemConfigUpdateRequest request) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new RuntimeException("配置项不存在: " + key));

        if (Boolean.FALSE.equals(config.getIsEditable())) {
            throw new RuntimeException("该配置项不可编辑");
        }

        if (request.getConfigValue() != null) config.setConfigValue(request.getConfigValue());
        if (request.getDescription() != null) config.setDescription(request.getDescription());
        if (request.getCategory() != null) config.setCategory(request.getCategory());
        if (request.getIsEditable() != null) config.setIsEditable(request.getIsEditable());
        if (request.getConfigType() != null) {
            config.setConfigType(SystemConfig.ConfigType.valueOf(request.getConfigType()));
        }

        config = configRepository.save(config);
        return SystemConfigResponse.from(config);
    }

    @Transactional
    public List<SystemConfigResponse> batchUpdate(SystemConfigUpdateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("批量更新项为空");
        }
        List<SystemConfig> updated = new ArrayList<>();
        for (Map.Entry<String, String> entry : request.getItems().entrySet()) {
            SystemConfig config = configRepository.findByConfigKey(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("配置项不存在: " + entry.getKey()));
            if (Boolean.FALSE.equals(config.getIsEditable())) {
                throw new RuntimeException("配置项不可编辑: " + entry.getKey());
            }
            config.setConfigValue(entry.getValue());
            updated.add(config);
        }
        List<SystemConfig> saved = configRepository.saveAll(updated);
        return saved.stream().map(SystemConfigResponse::from).collect(Collectors.toList());
    }
}
