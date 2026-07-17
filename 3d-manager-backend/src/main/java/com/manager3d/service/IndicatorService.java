package com.manager3d.service;

import com.manager3d.dto.request.IndicatorCreateRequest;
import com.manager3d.dto.response.IndicatorResponse;
import com.manager3d.entity.AnalysisTask;
import com.manager3d.entity.DataSource;
import com.manager3d.entity.Indicator;
import com.manager3d.entity.User;
import com.manager3d.repository.AnalysisTaskRepository;
import com.manager3d.repository.DataSourceRepository;
import com.manager3d.repository.IndicatorRepository;
import com.manager3d.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IndicatorService {

    private final IndicatorRepository indicatorRepository;
    private final UserRepository userRepository;
    private final DataSourceRepository dataSourceRepository;
    private final AnalysisTaskRepository analysisTaskRepository;

    @Transactional
    public IndicatorResponse createIndicator(IndicatorCreateRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Indicator.IndicatorBuilder builder = Indicator.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getTypeEnum())
                .valueType(request.getValueTypeEnum())
                .value(request.getValue())
                .owner(owner)
                .tags(request.getTags())
                .visibility(request.getVisibilityEnum());

        if (request.getDataSourceId() != null) {
            DataSource ds = dataSourceRepository.findById(request.getDataSourceId())
                    .orElseThrow(() -> new RuntimeException("数据源不存在"));
            builder.dataSource(ds);
        }
        if (request.getTaskId() != null) {
            AnalysisTask task = analysisTaskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new RuntimeException("分析任务不存在"));
            builder.task(task);
        }

        Indicator indicator = indicatorRepository.save(builder.build());
        return IndicatorResponse.from(indicator);
    }

    @Transactional(readOnly = true)
    public IndicatorResponse getIndicator(Long id) {
        Indicator indicator = indicatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在"));
        return IndicatorResponse.from(indicator);
    }

    @Transactional(readOnly = true)
    public Page<IndicatorResponse> listIndicators(String keyword, String type, Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Indicator.Type typeEnum = type != null ? Indicator.Type.valueOf(type) : null;
        Page<Indicator> indicatorPage = indicatorRepository.searchIndicators(keyword, typeEnum, ownerId, pageable);
        return indicatorPage.map(IndicatorResponse::from);
    }

    @Transactional
    public IndicatorResponse updateIndicator(Long id, IndicatorCreateRequest request) {
        Indicator indicator = indicatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在"));

        if (request.getName() != null) indicator.setName(request.getName());
        if (request.getDescription() != null) indicator.setDescription(request.getDescription());
        if (request.getType() != null) indicator.setType(request.getTypeEnum());
        if (request.getValueType() != null) indicator.setValueType(request.getValueTypeEnum());
        if (request.getValue() != null) indicator.setValue(request.getValue());
        if (request.getTags() != null) indicator.setTags(request.getTags());
        if (request.getVisibility() != null) indicator.setVisibility(request.getVisibilityEnum());

        if (request.getDataSourceId() != null) {
            DataSource ds = dataSourceRepository.findById(request.getDataSourceId())
                    .orElseThrow(() -> new RuntimeException("数据源不存在"));
            indicator.setDataSource(ds);
        }
        if (request.getTaskId() != null) {
            AnalysisTask task = analysisTaskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new RuntimeException("分析任务不存在"));
            indicator.setTask(task);
        }

        indicator = indicatorRepository.save(indicator);
        return IndicatorResponse.from(indicator);
    }

    @Transactional
    public void deleteIndicator(Long id) {
        if (!indicatorRepository.existsById(id)) {
            throw new RuntimeException("指标不存在");
        }
        indicatorRepository.deleteById(id);
    }
}
