package com.manager3d.service;

import com.manager3d.dto.request.TemplateCreateRequest;
import com.manager3d.dto.response.TemplateResponse;
import com.manager3d.entity.Template;
import com.manager3d.entity.User;
import com.manager3d.repository.TemplateRepository;
import com.manager3d.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<TemplateResponse> searchTemplates(String keyword, String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Template.Category categoryEnum = category != null ? Template.Category.valueOf(category) : null;
        Page<Template> templatePage = templateRepository.searchTemplates(keyword, categoryEnum, pageable);
        return templatePage.map(TemplateResponse::from);
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplate(Long id) {
        Template template = templateRepository.findByIdWithOwner(id)
                .orElseThrow(() -> new RuntimeException("模板不存在"));
        return TemplateResponse.from(template);
    }

    @Transactional
    public TemplateResponse createTemplate(TemplateCreateRequest request, String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Template template = Template.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory() != null ? Template.Category.valueOf(request.getCategory()) : Template.Category.SCENE)
                .config(request.getConfig())
                .previewImage(request.getPreviewImage())
                .isOfficial(request.getIsOfficial() != null ? request.getIsOfficial() : false)
                .useCount(0)
                .owner(owner)
                .build();

        template = templateRepository.save(template);
        return TemplateResponse.from(template);
    }

    @Transactional
    public TemplateResponse updateTemplate(Long id, TemplateCreateRequest request) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模板不存在"));

        if (request.getName() != null) template.setName(request.getName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getCategory() != null) template.setCategory(Template.Category.valueOf(request.getCategory()));
        if (request.getConfig() != null) template.setConfig(request.getConfig());
        if (request.getPreviewImage() != null) template.setPreviewImage(request.getPreviewImage());
        if (request.getIsOfficial() != null) template.setIsOfficial(request.getIsOfficial());

        template = templateRepository.save(template);
        return TemplateResponse.from(template);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模板不存在"));
        templateRepository.delete(template);
    }

    /**
     * 应用模板：递增useCount并返回模板config，前端用此config创建新场景/报表
     */
    @Transactional
    public Map<String, Object> applyTemplate(Long id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模板不存在"));
        template.setUseCount((template.getUseCount() == null ? 0 : template.getUseCount()) + 1);
        templateRepository.save(template);

        Map<String, Object> result = new HashMap<>();
        result.put("templateId", template.getId());
        result.put("templateName", template.getName());
        result.put("category", template.getCategory() != null ? template.getCategory().name() : null);
        result.put("config", template.getConfig());
        result.put("previewImage", template.getPreviewImage());
        return result;
    }
}
