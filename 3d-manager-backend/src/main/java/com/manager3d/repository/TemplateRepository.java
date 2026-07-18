package com.manager3d.repository;

import com.manager3d.entity.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    List<Template> findByCategory(Template.Category category);

    @Query("SELECT t FROM Template t LEFT JOIN FETCH t.owner " +
           "WHERE (:keyword IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:category IS NULL OR t.category = :category) " +
           "ORDER BY t.isOfficial DESC, t.useCount DESC, t.updatedAt DESC")
    Page<Template> searchTemplates(@Param("keyword") String keyword,
                                   @Param("category") Template.Category category,
                                   Pageable pageable);

    @Query("SELECT t FROM Template t LEFT JOIN FETCH t.owner WHERE t.id = :id")
    Optional<Template> findByIdWithOwner(@Param("id") Long id);
}
