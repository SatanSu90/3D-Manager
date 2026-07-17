package com.manager3d.repository;

import com.manager3d.entity.AnalysisTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AnalysisTaskRepository extends JpaRepository<AnalysisTask, Long> {

    @Query("SELECT t FROM AnalysisTask t LEFT JOIN FETCH t.owner WHERE t.id = :id")
    Optional<AnalysisTask> findByIdWithOwner(@Param("id") Long id);

    @EntityGraph(attributePaths = {"owner", "analysisSteps", "analysisDataSources", "analysisDataSources.dataSource"})
    @Query("SELECT t FROM AnalysisTask t WHERE t.id = :id")
    Optional<AnalysisTask> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT t FROM AnalysisTask t LEFT JOIN FETCH t.owner " +
           "WHERE (:keyword IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:ownerId IS NULL OR t.owner.id = :ownerId)")
    Page<AnalysisTask> searchTasks(@Param("keyword") String keyword,
                                   @Param("ownerId") Long ownerId,
                                   Pageable pageable);
}
