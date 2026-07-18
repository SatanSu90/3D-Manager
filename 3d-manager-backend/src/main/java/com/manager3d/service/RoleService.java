package com.manager3d.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manager3d.dto.request.RoleCreateRequest;
import com.manager3d.dto.response.RoleResponse;
import com.manager3d.entity.Role;
import com.manager3d.entity.User;
import com.manager3d.repository.RoleRepository;
import com.manager3d.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        // 查询每个角色的用户数
        Map<Long, Integer> userCountMap = new HashMap<>();
        for (Object[] row : roleRepository.countUsersByRole()) {
            Long roleId = (Long) row[0];
            Long count = (Long) row[1];
            userCountMap.put(roleId, count.intValue());
        }
        return roles.stream()
                .map(r -> RoleResponse.fromWithUserCount(r, userCountMap.getOrDefault(r.getId(), 0)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleResponse getRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse createRole(RoleCreateRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("角色编码已存在: " + request.getCode());
        }
        if (roleRepository.existsByName(request.getName())) {
            throw new RuntimeException("角色名称已存在: " + request.getName());
        }

        Role role = Role.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .permissions(serializePermissions(request.getPermissions()))
                .isSystem(false)
                .build();

        role = roleRepository.save(role);
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleCreateRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        if (request.getName() != null && !request.getName().equals(role.getName())
                && roleRepository.existsByName(request.getName())) {
            throw new RuntimeException("角色名称已存在: " + request.getName());
        }

        // 编码不可修改（系统角色约束）
        if (request.getCode() != null && !request.getCode().equals(role.getCode())) {
            throw new RuntimeException("角色编码不可修改");
        }

        if (request.getName() != null) role.setName(request.getName());
        if (request.getDescription() != null) role.setDescription(request.getDescription());
        if (request.getPermissions() != null) {
            role.setPermissions(serializePermissions(request.getPermissions()));
        }

        role = roleRepository.save(role);
        return RoleResponse.from(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("角色不存在"));

        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new RuntimeException("系统内置角色不可删除");
        }

        if (!role.getUsers().isEmpty()) {
            throw new RuntimeException("该角色下仍有用户，无法删除");
        }

        roleRepository.deleteById(id);
    }

    @Transactional
    public void assignRoleToUser(Long roleId, Long userId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Transactional
    public void removeRoleFromUser(Long roleId, Long userId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("角色不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.getRoles().remove(role);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<String> getUserPermissions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Set<String> permissions = new HashSet<>();
        for (Role role : user.getRoles()) {
            List<String> rolePermissions = deserializePermissions(role.getPermissions());
            permissions.addAll(rolePermissions);
        }
        return new ArrayList<>(permissions);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRoleUsers(Long roleId) {
        Role role = roleRepository.findByIdWithUsers(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        List<Map<String, Object>> users = new ArrayList<>();
        for (User user : role.getUsers()) {
            Map<String, Object> u = new LinkedHashMap<>();
            u.put("id", user.getId());
            u.put("username", user.getUsername());
            u.put("role", user.getRole().name());
            u.put("email", user.getEmail() != null ? user.getEmail() : "");
            u.put("phone", user.getPhone() != null ? user.getPhone() : "");
            u.put("status", user.getStatus().name());
            users.add(u);
        }
        return users;
    }

    private String serializePermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(permissions);
        } catch (JsonProcessingException e) {
            log.error("序列化权限失败", e);
            return "[]";
        }
    }

    private List<String> deserializePermissions(String permissions) {
        if (permissions == null || permissions.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(permissions, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析权限JSON失败: {}", permissions, e);
            return Collections.emptyList();
        }
    }
}
