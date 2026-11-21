package com.skr.erp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** A signed-in browser. The raw token is never stored, only its SHA-256 hash. */
@Entity
@Table(name = "user_session")
@Getter
@Setter
public class UserSession extends BaseEntity {

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    // The portfolio this session works in: the user's own, or the one a SUPER_ADMIN switched into.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_portfolio_id")
    private Portfolio activePortfolio;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    // Hard limit: the session ends here even if it is in use.
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
