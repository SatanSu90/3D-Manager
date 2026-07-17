package com.manager3d.service;

import com.manager3d.dto.request.UserProfileUpdateRequest;
import com.manager3d.entity.User;
import com.manager3d.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUserRole(Long id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setRole(User.Role.valueOf(role));
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Transactional
    public User updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = getUserById(userId);
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            if (request.getOldPassword() == null ||
                    !passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
                throw new RuntimeException("旧密码不正确");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateStatus(Long id, String status) {
        User user = getUserById(id);
        user.setStatus(User.Status.valueOf(status));
        return userRepository.save(user);
    }

    @Transactional
    public void updateLastLoginAt(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }
}
