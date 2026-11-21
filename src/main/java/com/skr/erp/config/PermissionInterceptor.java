package com.skr.erp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skr.erp.audit.AuditRequestFilter;
import com.skr.erp.common.constants.Role;
import com.skr.erp.entity.SystemSetting;
import com.skr.erp.entity.UserSession;
import com.skr.erp.exception.ForbiddenException;
import com.skr.erp.repository.SystemSettingRepository;
import com.skr.erp.security.CurrentUser;
import com.skr.erp.security.RequestContext;
import com.skr.erp.security.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

// Authentication + RBAC without Spring Security. Every API call carries a session token
// (Authorization: Bearer ...) plus userId & portfolioId query params. The token alone decides who
// the caller is and which portfolio they work in; the params must match it or the call is refused.
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private static final String BEARER = "Bearer ";
    private static final String ROLE_PERMISSIONS_KEY = "ROLE_PERMISSIONS";
    private static final Set<String> SUPER_ADMIN_SEGMENTS = Set.of("portfolios", "audit-logs");

    private final SessionService sessionService;
    private final SystemSettingRepository systemSettingRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (isPublic(method, path)) return true;

        UserSession session = sessionService.resolve(bearerToken(request));
        CurrentUser user = toCurrentUser(session);
        request.setAttribute(AuditRequestFilter.CURRENT_USER_ATTRIBUTE, user); // who, for the audit log (even if refused)
        verifyIdentityParams(request, user);
        // Set before authorize: permissions are per-portfolio settings, read through the portfolio filter.
        RequestContext.set(user);
        try {
            authorize(user, method, path);
        } catch (RuntimeException refused) {
            RequestContext.clear(); // afterCompletion doesn't run for a refused request
            throw refused;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        RequestContext.clear();
    }

    // Open endpoints: CORS preflight, login, and product images (plain <img src>, looked up by product id).
    private boolean isPublic(String method, String path) {
        if ("OPTIONS".equalsIgnoreCase(method)) return true;
        if ("POST".equalsIgnoreCase(method) && path.equals("/api/auth/login")) return true;
        return "GET".equalsIgnoreCase(method) && path.matches("/api/products/[^/]+/image");
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith(BEARER) ? header.substring(BEARER.length()).trim() : null;
    }

    private CurrentUser toCurrentUser(UserSession session) {
        UUID portfolioId = session.getActivePortfolio() != null ? session.getActivePortfolio().getId() : null;
        return new CurrentUser(session.getUser().getId(), session.getUser().getEmail(), session.getUser().getRole(),
                portfolioId, session.getTokenHash());
    }

    // userId / portfolioId sent by the client must be exactly the session's — never trusted on their own.
    private void verifyIdentityParams(HttpServletRequest request, CurrentUser user) {
        if (!user.userId().toString().equals(request.getParameter("userId"))) {
            throw new ForbiddenException("userId does not match the signed-in user");
        }
        String sent = request.getParameter("portfolioId");
        String expected = user.portfolioId() != null ? user.portfolioId().toString() : null;
        if (!Objects.equals(sent == null || sent.isBlank() ? null : sent, expected)) {
            throw new ForbiddenException("portfolioId does not match the signed-in portfolio");
        }
    }

    private void authorize(CurrentUser user, String method, String path) {
        String segment = ApiModules.firstSegment(path);
        if ("auth".equals(segment)) return; // own session: logout, me, switch portfolio, change password
        if (SUPER_ADMIN_SEGMENTS.contains(segment)) {
            // Portfolio management and audit logs: SUPER_ADMIN only, with or without a portfolio selected.
            if (!Role.SUPER_ADMIN.equals(user.role())) throw new ForbiddenException("Only a super admin can do this");
            return;
        }
        if (user.portfolioId() == null) throw new ForbiddenException("Select a portfolio first");
        if (Role.hasFullAccess(user.role())) return;

        String module = ApiModules.moduleForPath(path);
        String action = actionForMethod(method);
        if ("users".equals(segment)) throw new ForbiddenException("You do not have permission to manage users");
        // Settings are read app-wide to bootstrap the UI (currency, menu, permissions); writes are gated.
        if (module == null || ("settings".equals(module) && "view".equals(action))) return;
        if (!isAllowed(user.role(), module, action)) {
            throw new ForbiddenException("You do not have permission to " + action + " " + module);
        }
    }

    /** Look up ROLE_PERMISSIONS[role][module][action]; fail closed (false) on any problem. */
    @SuppressWarnings("unchecked")
    private boolean isAllowed(String role, String module, String action) {
        Optional<SystemSetting> setting = systemSettingRepository.findBySettingKey(ROLE_PERMISSIONS_KEY);
        if (setting.isEmpty() || setting.get().getSettingValue() == null) return false;
        try {
            Map<String, Map<String, Map<String, Boolean>>> perms =
                    objectMapper.readValue(setting.get().getSettingValue(), Map.class);
            Map<String, Map<String, Boolean>> rolePerms = perms.get(role);
            Map<String, Boolean> modulePerms = rolePerms != null ? rolePerms.get(module) : null;
            return modulePerms != null && Boolean.TRUE.equals(modulePerms.get(action));
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

}
