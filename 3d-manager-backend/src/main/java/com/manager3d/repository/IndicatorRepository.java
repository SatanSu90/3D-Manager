package com.manager3d.repository;

import com.manager3d.entity.Indicator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IndicatorRepository extends JpaRepository<Indicator, Long> {

    @Query("SELECT i FROM Indicator i LEFT JOIN FETCH i.owner " +
           "WHERE (:keyword IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:type IS NULL OR i.type = :type) " +
           "AND (:ownerId IS NULL OR i.owner.id = :ownerId)")
    Page<Indicator> searchIndicators(@Param("keyword") String keyword,
                                     @Param("type") Indicator.Type type,
                                     @Param("ownerId") Long ownerId,
                                     Pageable pageable);
}
