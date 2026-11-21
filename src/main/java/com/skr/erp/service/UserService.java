package com.skr.erp.service;

import com.skr.erp.common.constants.Role;
import com.skr.erp.dto.request.CreateUserRequest;
import com.skr.erp.dto.request.ResetPasswordRequest;
import com.skr.erp.dto.request.UpdateUserRequest;
import com.skr.erp.dto.response.UserResponse;
import com.skr.erp.entity.AppUser;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.AppUserRepository;
import com.skr.erp.repository.PortfolioRepository;
import com.skr.erp.security.PasswordService;
import com.skr.erp.security.RequestContext;
import com.skr.erp.security.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Users & Roles: an admin manages only the users of the portfolio it is working in. */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final AppUserRepository appUserRepository;
    private final PortfolioRepository portfolioRepository;
    private final PasswordService passwordService;
    private final SessionService sessionService;

    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return appUserRepository.findAllByPortfolioIdOrderByCreatedAtDesc(RequestContext.portfolioId())
                .stream()
                .map(this::map)
                .toList();
    }

    // New user in the current portfolio; email is the login and must be unique system-wide.
    public UserResponse create(CreateUserRequest request) {
        String role = normalizeRole(request.getRole());
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email already in use");
        }
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword(passwordService.hashNew(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setRole(role);
        user.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        user.setPortfolio(portfolioRepository.getReferenceById(RequestContext.portfolioId()));
        return map(appUserRepository.save(user));
    }

    public UserResponse update(UUID id, UpdateUserRequest request) {
        AppUser user = getOrThrow(id);
        String role = normalizeRole(request.getRole());
        boolean deactivating = Boolean.FALSE.equals(request.getActive()) && Boolean.TRUE.equals(user.getActive());
        // Don't allow the last active admin to be demoted/deactivated into lockout.
        if (Role.ADMIN.equals(user.getRole()) && (!Role.ADMIN.equals(role) || deactivating)) {
            ensureNotLastAdmin(user.getId());
        }
        user.setFullName(request.getFullName().trim());
        user.setRole(role);
        if (request.getActive() != null) user.setActive(request.getActive());
        if (deactivating) sessionService.closeAllFor(user.getId());
        return map(user);
    }

    // Admin sets a new password; the user is signed out everywhere.
    public UserResponse resetPassword(UUID id, ResetPasswordRequest request) {
        AppUser user = getOrThrow(id);
        user.setPassword(passwordService.hashNew(request.getNewPassword()));
        sessionService.closeAllFor(user.getId());
        return map(user);
    }

    public UserResponse toggleStatus(UUID id) {
        AppUser user = getOrThrow(id);
        boolean next = !Boolean.TRUE.equals(user.getActive());
        if (!next && Role.ADMIN.equals(user.getRole())) ensureNotLastAdmin(user.getId());
        user.setActive(next);
        if (!next) sessionService.closeAllFor(user.getId());
        return map(user);
    }

    private void ensureNotLastAdmin(UUID excludingId) {
        long otherActiveAdmins = appUserRepository.countByPortfolioIdAndRoleAndActiveTrueAndIdNot(
                RequestContext.portfolioId(), Role.ADMIN, excludingId);
        if (otherActiveAdmins == 0) {
            throw new BusinessException("At least one active admin is required");
        }
    }

    private String normalizeRole(String role) {
        String r = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!Role.isValid(r)) {
            throw new BusinessException("Role must be ADMIN or STAFF");
        }
        return r;
    }

    // Only users of the current portfolio are visible; another portfolio's id reads as "not found".
    private AppUser getOrThrow(UUID id) {
        return appUserRepository.findByIdAndPortfolioId(id, RequestContext.portfolioId())
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    private UserResponse map(AppUser user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.getActive())
                .build();
    }
}
