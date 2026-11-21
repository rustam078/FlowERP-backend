package com.skr.erp.security;

import com.skr.erp.exception.ForbiddenException;
import com.skr.erp.exception.InvalidCredentialsException;

import java.util.UUID;

/** Holds the CurrentUser for the running request (set and cleared by PermissionInterceptor). */
public final class RequestContext {

    /**
     * Current portfolio as a parameter for native SQL (Hibernate's automatic portfolio filter covers
     * JPQL/derived queries only). Space-padded because text blocks trim trailing spaces.
     */
    public static final String PORTFOLIO_SQL_PARAM = " :#{T(com.skr.erp.security.RequestContext).portfolioId()} ";

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(CurrentUser user) {
        CURRENT.set(user);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static CurrentUser current() {
        CurrentUser user = CURRENT.get();
        if (user == null) throw new InvalidCredentialsException("Not authenticated");
        return user;
    }

    // Portfolio every read/write of this request is limited to.
    public static UUID portfolioId() {
        UUID id = current().portfolioId();
        if (id == null) throw new ForbiddenException("Select a portfolio first");
        return id;
    }

    // Same, but null outside a signed-in request (used by the Hibernate tenant resolver).
    public static UUID portfolioIdOrNull() {
        CurrentUser user = CURRENT.get();
        return user != null ? user.portfolioId() : null;
    }
}
