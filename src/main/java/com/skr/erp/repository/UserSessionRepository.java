package com.skr.erp.repository;

import com.skr.erp.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    // Session + user + both portfolios in one query (runs on every API call).
    @Query("""
            SELECT s FROM UserSession s
            JOIN FETCH s.user u
            LEFT JOIN FETCH u.portfolio
            LEFT JOIN FETCH s.activePortfolio
            WHERE s.tokenHash = :tokenHash
            """)
    Optional<UserSession> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.tokenHash = :tokenHash")
    void deleteByTokenHash(@Param("tokenHash") String tokenHash);

    // Signs a user out everywhere (deactivated, password reset/changed).
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    // Portfolio deleted: sign out all of its users at once.
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.user.id IN (SELECT u.id FROM AppUser u WHERE u.portfolio.id = :portfolioId)")
    void deleteAllByPortfolioId(@Param("portfolioId") UUID portfolioId);

    // After a password change: keep the current browser, sign out every other one.
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.user.id = :userId AND s.tokenHash <> :keepTokenHash")
    void deleteOtherSessions(@Param("userId") UUID userId, @Param("keepTokenHash") String keepTokenHash);

    // Housekeeping: drop sessions past their hard limit or idle timeout.
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now OR s.lastSeenAt < :idleBefore")
    int deleteExpired(@Param("now") LocalDateTime now, @Param("idleBefore") LocalDateTime idleBefore);
}
