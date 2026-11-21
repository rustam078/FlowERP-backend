package com.skr.erp.audit;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Full audit entry: the request row, its portfolio name, and every row it changed (before / after). */
@Data
@Builder
public class AuditLogDetailResponse {
    private AuditLog log;
    private String ref;
    private String portfolioName;
    private List<AuditLogChange> changes;
}
