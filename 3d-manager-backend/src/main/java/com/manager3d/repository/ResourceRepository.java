package com.manager3d.repository;

import com.manager3d.entity.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r LEFT JOIN FETCH r.owner " +
           "WHERE (:keyword IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "      OR LOWER(r.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:type IS NULL OR r.type = :type) " +
           "AND (:ownerId IS NULL OR r.owner.id = :ownerId) " +
           "ORDER BY r.createdAt DESC")
    Page<Resource> searchResources(@Param("keyword") String keyword,
                                   @Param("type") Resource.Type type,
                                   @Param("ownerId") Long ownerId,
                                   Pageable pageable);

    @Query("SELECT r FROM Resource r LEFT JOIN FETCH r.owner WHERE r.id = :id")
    Optional<Resource> findByIdWithOwner(@Param("id") Long id);
}
