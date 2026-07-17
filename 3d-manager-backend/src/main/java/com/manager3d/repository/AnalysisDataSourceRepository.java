package com.manager3d.repository;

import com.manager3d.entity.AnalysisDataSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisDataSourceRepository extends JpaRepository<AnalysisDataSource, Long> {

    List<AnalysisDataSource> findByTaskId(Long taskId);
}
