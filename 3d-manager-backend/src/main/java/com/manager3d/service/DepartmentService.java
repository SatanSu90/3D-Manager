package com.manager3d.service;

import com.manager3d.dto.request.DepartmentCreateRequest;
import com.manager3d.dto.response.DepartmentResponse;
import com.manager3d.entity.Department;
import com.manager3d.entity.User;
import com.manager3d.repository.DepartmentRepository;
import com.manager3d.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAllByOrderBySortOrder().stream()
                .map(DepartmentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartmentTree() {
        List<Department> allDepts = departmentRepository.findAllByOrderBySortOrder();
        // 构建树
        Map<Long, List<Department>> childrenMap = allDepts.stream()
                .filter(d -> d.getParent() != null)
                .collect(Collectors.groupingBy(d -> d.getParent().getId()));

        List<Department> roots = allDepts.stream()
                .filter(d -> d.getParent() == null)
                .collect(Collectors.toList());

        // 递归设置children
        for (Department root : roots) {
            setChildren(root, childrenMap);
        }

        return roots.stream()
                .map(DepartmentResponse::fromWithChildren)
                .collect(Collectors.toList());
    }

    private void setChildren(Department dept, Map<Long, List<Department>> childrenMap) {
        List<Department> children = childrenMap.get(dept.getId());
        if (children != null) {
            dept.setChildren(children);
            for (Department child : children) {
                setChildren(child, childrenMap);
            }
        }
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        return DepartmentResponse.from(dept);
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentCreateRequest request) {
        Department parent = null;
        if (request.getParentId() != null) {
            parent = departmentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("上级部门不存在"));
        }

        Department dept = Department.builder()
                .name(request.getName())
                .parent(parent)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();

        dept = departmentRepository.save(dept);
        return DepartmentResponse.from(dept);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentCreateRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("部门不存在"));

        if (request.getName() != null) dept.setName(request.getName());
        if (request.getSortOrder() != null) dept.setSortOrder(request.getSortOrder());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new RuntimeException("不能将自身设为上级部门");
            }
            Department parent = departmentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("上级部门不存在"));
            dept.setParent(parent);
        }

        dept = departmentRepository.save(dept);
        return DepartmentResponse.from(dept);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        if (departmentRepository.existsByParentId(id)) {
            throw new RuntimeException("该部门下有子部门，无法删除");
        }
        if (!departmentRepository.existsById(id)) {
            throw new RuntimeException("部门不存在");
        }
        departmentRepository.deleteById(id);
    }

    @Transactional
    public void assignUsers(Long departmentId, List<Long> userIds) {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));

        List<User> users = userRepository.findAllById(userIds);
        for (User user : users) {
            user.getDepartments().add(dept);
        }
        userRepository.saveAll(users);
    }

    @Transactional
    public void removeUser(Long departmentId, Long userId) {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.getDepartments().remove(dept);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDepartmentUsers(Long departmentId) {
        Department dept = departmentRepository.findByIdWithUsers(departmentId);
        if (dept == null) {
            throw new RuntimeException("部门不存在");
        }
        List<Map<String, Object>> users = new ArrayList<>();
        for (User user : dept.getUsers()) {
            Map<String, Object> u = Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "role", user.getRole().name(),
                    "email", user.getEmail() != null ? user.getEmail() : "",
                    "phone", user.getPhone() != null ? user.getPhone() : "",
                    "status", user.getStatus().name()
            );
            users.add(u);
        }
        return users;
    }
}
