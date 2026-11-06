package com.skr.erp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skr.erp.common.constants.Role;
import com.skr.erp.entity.AppUser;
import com.skr.erp.entity.SystemSetting;
import com.skr.erp.exception.ForbiddenException;
import com.skr.erp.exception.InvalidCredentialsException;
import com.skr.erp.repository.AppUserRepository;
import com.skr.erp.repository.SystemSettingRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Session-style RBAC without Spring Security. The logged-in user id travels in the
// X-User-Id header; here we load the user, and for STAFF we check the requested
// module + action against the ROLE_PERMISSIONS setting. ADMIN always passes.
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private static final String USER_HEADER = "X-User-Id";
    private static final String ROLE_PERMISSIONS_KEY = "ROLE_PERMISSIONS";

    private final AppUserRepository appUserRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        String method = request.getMethod();
        String path = request.getRequestURI();

        // CORS preflight and the login/change-password endpoints are open.
        if ("OPTIONS".equalsIgnoreCase(method) || path.startsWith("/api/auth/")) {
            return true;
        }

        // Product images are public on GET so plain <img src> tags can load them (no auth header).
        if ("GET".equalsIgnoreCase(method) && path.matches("/api/products/[^/]+/image")) {
            return true;
        }

        // Company name is public (shown on the login page before sign-in). Not sensitive.
        if ("GET".equalsIgnoreCase(method) && path.equals("/api/settings/COMPANY_NAME")) {
            return true;
        }

        AppUser user = resolveUser(request);

        // ADMIN has full access.
        if (Role.ADMIN.equals(user.getRole())) {
            return true;
        }

        // Everything below is STAFF (or an unknown role, treated as restricted).
        String module = moduleForPath(path);
        String action = actionForMethod(method);

        // User administration is ADMIN-only.
        if ("users".equals(firstSegment(path))) {
            throw new ForbiddenException("You do not have permission to manage users");
        }

        // Reading the settings store (currency, menu, permissions, company info) is needed
        // app-wide to bootstrap the UI, so any signed-in user may GET it. Writes are gated.
        if ("settings".equals(module) && "view".equals(action)) {
            return true;
        }

        // Unmapped /api paths: allow (no known module to gate).
        if (module == null) {
            return true;
        }

        if (!isAllowed(user.getRole(), module, action)) {
            throw new ForbiddenException("You do not have permission to " + action + " " + module);
        }
        return true;
    }

    private AppUser resolveUser(HttpServletRequest request) {
        String header = request.getHeader(USER_HEADER);
        if (header == null || header.isBlank()) {
            throw new InvalidCredentialsException("Not authenticated");
        }
        UUID id;
        try {
            id = UUID.fromString(header.trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidCredentialsException("Invalid session");
        }
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new InvalidCredentialsException("Session expired, please sign in again"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new InvalidCredentialsException("Account is inactive");
        }
        return user;
    }

    /** Look up ROLE_PERMISSIONS[role][module][action]; fail closed (false) on any problem. */
    private boolean isAllowed(String role, String module, String action) {
        Optional<SystemSetting> setting = systemSettingRepository.findBySettingKey(ROLE_PERMISSIONS_KEY);
        if (setting.isEmpty() || setting.get().getSettingValue() == null) {
            return false;
        }
        try {
            Map<String, Map<String, Map<String, Boolean>>> perms =
                    objectMapper.readValue(setting.get().getSettingValue(), Map.class);
            Map<String, Map<String, Boolean>> rolePerms = perms.get(role);
            if (rolePerms == null) return false;
            Map<String, Boolean> modulePerms = rolePerms.get(module);
            if (modulePerms == null) return false;
            return Boolean.TRUE.equals(modulePerms.get(action));
        } catch (Exception e) {
            return false;
        }
    }

    private String actionForMethod(String method) {
        return switch (method.toUpperCase()) {
            case "GET", "HEAD" -> "view";
            case "DELETE" -> "delete";
            default -> "write"; // POST, PUT, PATCH
        };
    }

    private String firstSegment(String path) {
        // path like /api/<segment>/...
        String p = path.startsWith("/api/") ? path.substring(5) : path;
        int slash = p.indexOf('/');
        return slash >= 0 ? p.substring(0, slash) : p;
    }

    private String moduleForPath(String path) {
        String seg = firstSegment(path);
        return switch (seg) {
            case "sales", "customers", "qr" -> "sales";
            case "inventory" -> "inventory";
            case "products", "product-material-cost" -> "products";
            case "investments", "vendors" -> "investment";
            case "production" -> "production";
            case "employees" -> "employees";
            case "dashboard" -> "dashboard";
            case "settings", "users" -> "settings";
            default -> null;
        };
    }
}
