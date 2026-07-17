package com.manager3d.repository;

import com.manager3d.entity.Scene;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SceneRepository extends JpaRepository<Scene, Long> {

    Page<Scene> findByCreatorIdOrderByUpdatedAtDesc(Long creatorId, Pageable pageable);

    @Query("SELECT s FROM Scene s JOIN FETCH s.creator WHERE s.id = :id")
    Optional<Scene> findByIdWithCreator(@Param("id") Long id);

    @Query("SELECT s FROM Scene s JOIN FETCH s.creator WHERE s.creator.id = :creatorId ORDER BY s.updatedAt DESC")
    List<Scene> findByCreatorIdWithCreator(@Param("creatorId") Long creatorId);

    @Query("SELECT s FROM Scene s JOIN FETCH s.creator LEFT JOIN FETCH s.category LEFT JOIN FETCH s.owner " +
           "WHERE (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:categoryId IS NULL OR s.category.id = :categoryId) " +
           "AND (:status IS NULL OR s.status = :status) " +
           "AND (s.owner.id = :userId OR s.visibility != 'PRIVATE') " +
           "ORDER BY s.updatedAt DESC")
    Page<Scene> searchScenes(@Param("keyword") String keyword,
                             @Param("categoryId") Long categoryId,
                             @Param("status") Scene.Status status,
                             @Param("userId") Long userId,
                             Pageable pageable);
}
