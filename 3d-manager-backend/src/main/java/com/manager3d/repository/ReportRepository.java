package com.manager3d.repository;

import com.manager3d.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.owner " +
           "WHERE (:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:type IS NULL OR r.type = :type) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:ownerId IS NULL OR r.owner.id = :ownerId) " +
           "ORDER BY r.updatedAt DESC")
    Page<Report> searchReports(@Param("keyword") String keyword,
                               @Param("type") Report.Type type,
                               @Param("status") Report.Status status,
                               @Param("ownerId") Long ownerId,
                               Pageable pageable);

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.owner WHERE r.id = :id")
    Optional<Report> findByIdWithOwner(@Param("id") Long id);
}
