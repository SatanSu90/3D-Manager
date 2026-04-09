package com.manager3d.service;

import com.manager3d.dto.request.CategoryCreateRequest;
import com.manager3d.entity.Category;
import com.manager3d.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findByParentIdIsNullOrderBySortOrder();
    }

    @Transactional
    public Category createCategory(CategoryCreateRequest request) {
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId()).orElse(null);
        }

        Category category = Category.builder()
                .name(request.getName())
                .parent(parent)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, CategoryCreateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        if (request.getName() != null) category.setName(request.getName());
        if (request.getSortOrder() != null) category.setSortOrder(request.getSortOrder());
        if (request.getParentId() != null) {
            category.setParent(categoryRepository.findById(request.getParentId()).orElse(null));
        }

        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public List<Map<String, Object>> getCategoryTree() {
        List<Map<String, Object>> rows = categoryRepository.getCategoryTree();
        return rows.stream().map(row -> {
            Map<String, Object> mapped = new java.util.LinkedHashMap<>();
            mapped.put("id", row.get("id"));
            mapped.put("name", row.get("name"));
            mapped.put("parentId", row.get("parent_id"));
            mapped.put("sortOrder", row.get("sort_order"));
            mapped.put("modelCount", row.get("model_count"));
            return mapped;
        }).collect(java.util.stream.Collectors.toList());
       }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }
}
