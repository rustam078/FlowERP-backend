package com.skr.erp.audit;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Audit log search (all optional). Named filterPortfolioId because "portfolioId" is the caller's own
 * identity parameter checked on every request.
 */
@Data
public class AuditLogFilter {

    private UUID filterPortfolioId;

    private String module;

    private String outcome; // SUCCESS (status < 400) or FAILED

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private String search; // request id (Ref), action, URL or user email

    private UUID entityId; // every request that changed this record
}
