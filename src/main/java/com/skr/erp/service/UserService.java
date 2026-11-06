package com.skr.erp.service;

import com.skr.erp.common.constants.Role;
import com.skr.erp.dto.request.CreateUserRequest;
import com.skr.erp.dto.request.ResetPasswordRequest;
import com.skr.erp.dto.request.UpdateUserRequest;
import com.skr.erp.dto.response.UserResponse;
import com.skr.erp.entity.AppUser;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return appUserRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::map)
                .toList();
    }

    public UserResponse create(CreateUserRequest request) {

        String role = normalizeRole(request.getRole());

        if (appUserRepository.existsByUsernameIgnoreCase(request.getUsername().trim())) {
            throw new BusinessException("Username already exists");
        }
        if (request.getPassword().length() < 4) {
            throw new BusinessException("Password must be at least 4 characters");
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername().trim());
        user.setPassword(request.getPassword()); // plaintext by decision
        user.setFullName(request.getFullName().trim());
        user.setRole(role);
        user.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());

        return map(appUserRepository.save(user));
    }

    public UserResponse update(UUID id, UpdateUserRequest request) {

        AppUser user = getOrThrow(id);
        String role = normalizeRole(request.getRole());

        // Don't allow the last active admin to be demoted/deactivated into lockout.
        if (Role.ADMIN.equals(user.getRole()) && !Role.ADMIN.equals(role)) {
            ensureNotLastAdmin(user.getId());
        }

        user.setFullName(request.getFullName().trim());
        user.setRole(role);
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        return map(appUserRepository.save(user));
    }

    public UserResponse resetPassword(UUID id, ResetPasswordRequest request) {
        AppUser user = getOrThrow(id);
        if (request.getNewPassword().length() < 4) {
            throw new BusinessException("Password must be at least 4 characters");
        }
        user.setPassword(request.getNewPassword());
        return map(appUserRepository.save(user));
    }

    public UserResponse toggleStatus(UUID id) {
        AppUser user = getOrThrow(id);
        boolean next = !Boolean.TRUE.equals(user.getActive());
        // Prevent deactivating the last active admin.
        if (!next && Role.ADMIN.equals(user.getRole())) {
            ensureNotLastAdmin(user.getId());
        }
        user.setActive(next);
        return map(appUserRepository.save(user));
    }

    private void ensureNotLastAdmin(UUID excludingId) {
        long otherActiveAdmins = appUserRepository.findAll().stream()
                .filter(u -> Role.ADMIN.equals(u.getRole()))
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .filter(u -> !u.getId().equals(excludingId))
                .count();
        if (otherActiveAdmins == 0) {
            throw new BusinessException("At least one active admin is required");
        }
    }

    private String normalizeRole(String role) {
        String r = role == null ? "" : role.trim().toUpperCase();
        if (!Role.isValid(r)) {
            throw new BusinessException("Role must be ADMIN or STAFF");
        }
        return r;
    }

    private AppUser getOrThrow(UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    private UserResponse map(AppUser user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.getActive())
                .build();
    }
}
