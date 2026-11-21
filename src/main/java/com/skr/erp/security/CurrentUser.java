package com.skr.erp.security;

import java.util.UUID;

/**
 * Who is calling and in which portfolio, resolved on the server from the session token.
 * portfolioId is null only for a SUPER_ADMIN who has not picked a portfolio yet.
 */
public record CurrentUser(UUID userId, String email, String role, UUID portfolioId, String tokenHash) {
}
