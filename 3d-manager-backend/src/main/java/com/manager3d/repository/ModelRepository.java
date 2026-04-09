package com.manager3d.repository;

import com.manager3d.entity.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {

    @Query("SELECT m FROM Model m " +
           "LEFT JOIN FETCH m.category " +
           "LEFT JOIN FETCH m.uploader " +
           "LEFT JOIN FETCH m.tags " +
           "WHERE (:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR m.category.id = :categoryId) " +
           "AND (:uploaderId IS NULL OR m.uploader.id = :uploaderId)")
    Page<Model> searchModels(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("uploaderId") Long uploaderId,
            Pageable pageable
    );

    @Query("SELECT m FROM Model m LEFT JOIN FETCH m.category LEFT JOIN FETCH m.uploader LEFT JOIN FETCH m.tags WHERE m.id = :id")
    java.util.Optional<Model> findByIdWithDetails(@Param("id") Long id);
}
