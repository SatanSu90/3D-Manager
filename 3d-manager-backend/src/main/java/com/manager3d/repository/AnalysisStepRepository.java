package com.manager3d.repository;

import com.manager3d.entity.AnalysisStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisStepRepository extends JpaRepository<AnalysisStep, Long> {

    List<AnalysisStep> findByTaskIdOrderByStepOrder(Long taskId);
}
