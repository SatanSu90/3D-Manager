package com.manager3d.repository;

import com.manager3d.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIdIsNullOrderBySortOrder();

    List<Category> findByParentIdOrderBySortOrder(Long parentId);

    @Query(value = "SELECT c.id, c.name, c.parent_id, c.sort_order, " +
            "(SELECT COUNT(m.id) FROM `model` m WHERE m.category_id = c.id) as model_count " +
            "FROM `category` c ORDER BY c.sort_order", nativeQuery = true)
    List<Map<String, Object>> getCategoryTree();
}
