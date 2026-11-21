package com.skr.erp.audit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row of the audit log list (large JSON bodies are only in the detail view). */
@Data
@Builder
public class AuditLogSummaryResponse {
    private UUID id;
    private String ref;           // short request id, as shown to users in error messages
    private LocalDateTime createdAt;
    private UUID portfolioId;
    private String portfolioName;
    private String userEmail;
    private String userRole;
    private String action;
    private String module;
    private String httpMethod;
    private String urlPath;
    private Integer responseStatus;
    private Integer durationMs;
    private String errorMessage;
    private long changeCount;
}
