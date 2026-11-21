package com.skr.erp.audit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/** One audited request (not portfolio-filtered: only the super admin reads it, across portfolios). */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email", length = 150)
    private String userEmail;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Column(nullable = false, length = 150)
    private String action;

    @Column(length = 40)
    private String module;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "url_path", nullable = false, length = 500)
    private String urlPath;

    @Column(name = "url_pattern", length = 300)
    private String urlPattern;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "query_params")
    private JsonNode queryParams;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_body")
    private JsonNode requestBody;

    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body")
    private JsonNode responseBody;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "duration_ms", nullable = false)
    private Integer durationMs;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
