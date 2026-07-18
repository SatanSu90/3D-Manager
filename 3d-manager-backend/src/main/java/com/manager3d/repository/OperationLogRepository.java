package com.manager3d.repository;

import com.manager3d.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    @Query("SELECT l FROM OperationLog l WHERE " +
            "(:module IS NULL OR l.module = :module) AND " +
            "(:action IS NULL OR l.action = :action) AND " +
            "(:userId IS NULL OR l.userId = :userId) AND " +
            "(:status IS NULL OR l.status = :status) AND " +
            "(:startDate IS NULL OR l.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR l.createdAt <= :endDate) " +
            "ORDER BY l.createdAt DESC")
    Page<OperationLog> search(@Param("module") String module,
                              @Param("action") String action,
                              @Param("userId") Long userId,
                              @Param("status") OperationLog.Status status,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              Pageable pageable);

    long deleteByCreatedAtBefore(LocalDateTime before);
}
