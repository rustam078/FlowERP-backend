package com.skr.erp.audit;

import com.skr.erp.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Everything the filter saw about one audited request (parameter object for AuditRecorder). */
record AuditCapture(UUID requestId, String action, String httpMethod, String path, String urlPattern,
                    Map<String, String[]> params, byte[] requestBody, String contentType,
                    int status, byte[] responseBody, LocalDateTime startedAt, long durationMs,
                    CurrentUser user, String clientIp, String userAgent, List<EntityChange> changes) {
}
