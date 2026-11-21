package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** The signed-in session: token (login only), user, and the portfolio being worked in. */
@Data
@Builder
public class LoginResponse {
    private String token;
    private UUID userId;
    private String email;
    private String fullName;
    private String role;
    private UUID portfolioId;     // null for a SUPER_ADMIN who has not picked a portfolio
    private String portfolioName;
}
