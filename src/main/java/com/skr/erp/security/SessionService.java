package com.skr.erp.security;

import com.skr.erp.common.constants.Role;
import com.skr.erp.entity.AppUser;
import com.skr.erp.entity.Portfolio;
import com.skr.erp.entity.UserSession;
import com.skr.erp.exception.InvalidCredentialsException;
import com.skr.erp.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Server-side login sessions: a random token goes to the client, only its SHA-256 hash is stored.
 * A session ends after 12 h without use, or 7 days after sign-in, or on logout / deactivation.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    private static final Duration IDLE_TIMEOUT = Duration.ofHours(12);
    private static final Duration MAX_AGE = Duration.ofDays(7);
    private static final Duration TOUCH_INTERVAL = Duration.ofMinutes(5);
    private static final String EXPIRED = "Session expired, please sign in again";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserSessionRepository userSessionRepository;

    // Opens a session for a signed-in user and returns the raw token (given to the client once).
    @Transactional
    public String open(AppUser user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();
        UserSession session = new UserSession();
        session.setTokenHash(hash(token));
        session.setUser(user);
        session.setActivePortfolio(user.getPortfolio());
        session.setLastSeenAt(now);
        session.setExpiresAt(now.plus(MAX_AGE));
        userSessionRepository.save(session);
        return token;
    }

    // The live session for a raw token, else 401. Refreshes last-seen at most every 5 minutes.
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public UserSession resolve(String token) {
        if (token == null || token.isBlank()) throw new InvalidCredentialsException("Not authenticated");
        UserSession session = userSessionRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new InvalidCredentialsException(EXPIRED));
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(session.getExpiresAt()) || now.isAfter(session.getLastSeenAt().plus(IDLE_TIMEOUT))) {
            userSessionRepository.delete(session);
            throw new InvalidCredentialsException(EXPIRED);
        }
        ensureStillAllowed(session);
        if (now.isAfter(session.getLastSeenAt().plus(TOUCH_INTERVAL))) session.setLastSeenAt(now);
        return session;
    }

    @Transactional
    public void close(String tokenHash) {
        userSessionRepository.deleteByTokenHash(tokenHash);
    }

    // Signs a user out on every device (deactivated, password reset).
    @Transactional
    public void closeAllFor(UUID userId) {
        userSessionRepository.deleteAllByUserId(userId);
    }

    @Transactional
    public void purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        userSessionRepository.deleteExpired(now, now.minus(IDLE_TIMEOUT));
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // A user or portfolio switched off after sign-in loses access on the next request.
    private void ensureStillAllowed(UserSession session) {
        AppUser user = session.getUser();
        if (!Boolean.TRUE.equals(user.getActive())) {
            userSessionRepository.delete(session);
            throw new InvalidCredentialsException("Account is inactive");
        }
        Portfolio active = session.getActivePortfolio();
        if (active == null || active.isUsable()) return;
        if (Role.SUPER_ADMIN.equals(user.getRole())) {
            session.setActivePortfolio(null); // back to the portfolio picker
            return;
        }
        userSessionRepository.delete(session);
        throw new InvalidCredentialsException("Your portfolio is not active");
    }
}
