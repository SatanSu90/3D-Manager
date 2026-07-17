package com.manager3d.repository;

import com.manager3d.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByParentIdIsNullOrderBySortOrder();

    List<Department> findByParentIdOrderBySortOrder(Long parentId);

    List<Department> findAllByOrderBySortOrder();

    boolean existsByParentId(Long parentId);

    @Query("SELECT d FROM Department d LEFT JOIN FETCH d.users WHERE d.id = :id")
    Department findByIdWithUsers(Long id);
}
