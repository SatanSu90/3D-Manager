package com.manager3d.repository;

import com.manager3d.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.users WHERE r.id = :id")
    Role findByIdWithUsers(Long id);

    @Query("SELECT r.id, COUNT(u) FROM Role r LEFT JOIN r.users u WHERE u.id IS NOT NULL GROUP BY r.id")
    List<Object[]> countUsersByRole();
}
