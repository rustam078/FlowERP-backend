package com.skr.erp.service;

import com.skr.erp.common.constants.Role;
import com.skr.erp.dto.request.ChangePasswordRequest;
import com.skr.erp.dto.request.LoginRequest;
import com.skr.erp.dto.request.SwitchPortfolioRequest;
import com.skr.erp.dto.response.LoginResponse;
import com.skr.erp.entity.AppUser;
import com.skr.erp.entity.Portfolio;
import com.skr.erp.entity.UserSession;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.exception.ForbiddenException;
import com.skr.erp.exception.InvalidCredentialsException;
import com.skr.erp.repository.AppUserRepository;
import com.skr.erp.repository.PortfolioRepository;
import com.skr.erp.repository.UserSessionRepository;
import com.skr.erp.security.CurrentUser;
import com.skr.erp.security.PasswordService;
import com.skr.erp.security.RequestContext;
import com.skr.erp.security.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sign in / out, who-am-I, SUPER_ADMIN portfolio switching, and own password change. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BAD_LOGIN = "Invalid email or password";

    private final AppUserRepository appUserRepository;
    private final PortfolioRepository portfolioRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordService passwordService;
    private final SessionService sessionService;

    // Email + password → new session token. Same message for unknown email and wrong password.
    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmailWithPortfolio(request.getEmail().trim())
                .orElseThrow(() -> new InvalidCredentialsException(BAD_LOGIN));
        if (!passwordService.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException(BAD_LOGIN);
        }
        if (!Boolean.TRUE.equals(user.getActive())) throw new InvalidCredentialsException("User is inactive");
        boolean superAdmin = Role.SUPER_ADMIN.equals(user.getRole());
        if (!superAdmin && (user.getPortfolio() == null || !user.getPortfolio().isUsable())) {
            throw new InvalidCredentialsException("Your portfolio is not active");
        }
        sessionService.purgeExpired();
        String token = sessionService.open(user);
        return toResponse(token, user, user.getPortfolio());
    }

    @Transactional
    public void logout() {
        sessionService.close(RequestContext.current().tokenHash());
    }

    // Current session details (used by the UI after a reload or a portfolio switch).
    @Transactional(readOnly = true)
    public LoginResponse me() {
        UserSession session = currentSession();
        return toResponse(null, session.getUser(), session.getActivePortfolio());
    }

    // SUPER_ADMIN only: work inside another portfolio (null = back to the picker).
    @Transactional
    public LoginResponse switchPortfolio(SwitchPortfolioRequest request) {
        if (!Role.SUPER_ADMIN.equals(RequestContext.current().role())) {
            throw new ForbiddenException("Only a super admin can switch portfolios");
        }
        Portfolio target = request.getPortfolioId() == null ? null
                : portfolioRepository.findById(request.getPortfolioId()).filter(Portfolio::isUsable)
                        .orElseThrow(() -> new BusinessException("Portfolio not found or inactive"));
        UserSession session = currentSession();
        session.setActivePortfolio(target);
        return toResponse(null, session.getUser(), target);
    }

    // BusinessException (409), not 401, so a wrong current password doesn't log the user out.
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        CurrentUser current = RequestContext.current();
        AppUser user = appUserRepository.findById(current.userId())
                .orElseThrow(() -> new BusinessException("User not found"));
        if (!passwordService.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        user.setPassword(passwordService.hashNew(request.getNewPassword()));
        userSessionRepository.deleteOtherSessions(user.getId(), current.tokenHash()); // other devices sign out
    }

    private UserSession currentSession() {
        return userSessionRepository.findByTokenHash(RequestContext.current().tokenHash())
                .orElseThrow(() -> new InvalidCredentialsException("Session expired, please sign in again"));
    }

    private LoginResponse toResponse(String token, AppUser user, Portfolio portfolio) {
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .portfolioId(portfolio != null ? portfolio.getId() : null)
                .portfolioName(portfolio != null ? portfolio.getName() : null)
                .build();
    }
}
