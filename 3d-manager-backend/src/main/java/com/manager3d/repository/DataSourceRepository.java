package com.manager3d.repository;

import com.manager3d.entity.DataSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DataSourceRepository extends JpaRepository<DataSource, Long> {

    Page<DataSource> findByOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT ds FROM DataSource ds " +
           "LEFT JOIN FETCH ds.owner " +
           "WHERE ds.owner.id = :ownerId " +
           "AND (:keyword IS NULL OR LOWER(ds.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:type IS NULL OR ds.type = :type)")
    Page<DataSource> searchDataSources(@Param("keyword") String keyword,
                                       @Param("type") DataSource.Type type,
                                       @Param("ownerId") Long ownerId,
                                       Pageable pageable);

    @Query("SELECT ds FROM DataSource ds LEFT JOIN FETCH ds.owner WHERE ds.id = :id")
    Optional<DataSource> findByIdWithOwner(@Param("id") Long id);
}
