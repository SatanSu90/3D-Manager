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
}
