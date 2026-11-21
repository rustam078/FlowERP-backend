package com.skr.erp.audit;

import com.skr.erp.config.ApiModules;
import com.skr.erp.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Writes one audit row (+ its row changes) in its own transaction, independent of the business one. */
@Service
@RequiredArgsConstructor
class AuditRecorder {

    private static final String LOGIN_PATH = "/api/auth/login";

    private final AuditLogRepository auditLogRepository;
    private final AuditLogChangeRepository auditLogChangeRepository;
    private final AuditPayloadSanitizer sanitizer;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditCapture capture) {
        AuditLog log = toLog(capture);
        auditLogRepository.save(log);
        for (EntityChange change : capture.changes()) auditLogChangeRepository.save(toChange(log, change));
    }

    private AuditLog toLog(AuditCapture c) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID());
        log.setRequestId(c.requestId());
        log.setAction(c.action());
        log.setModule(c.path().equals(LOGIN_PATH) ? "auth" : moduleOf(c.path()));
        log.setHttpMethod(c.httpMethod());
        log.setUrlPath(truncate(c.path(), 500));
        log.setUrlPattern(truncate(c.urlPattern(), 300));
        log.setQueryParams(sanitizer.params(c.params()));
        log.setRequestBody(sanitizer.request(c.requestBody(), c.contentType()));
        log.setResponseStatus(c.status());
        log.setResponseBody(sanitizer.response(c.responseBody(), c.status()));
        log.setErrorMessage(sanitizer.errorMessage(c.responseBody(), c.status()));
        log.setDurationMs((int) Math.min(c.durationMs(), Integer.MAX_VALUE));
        log.setClientIp(truncate(c.clientIp(), 45));
        log.setUserAgent(truncate(c.userAgent(), 300));
        log.setCreatedAt(c.startedAt());
        setWho(log, c);
        return log;
    }

    // Signed-in caller; for a login attempt, the email that was tried (and the user id if it succeeded).
    private void setWho(AuditLog log, AuditCapture c) {
        CurrentUser user = c.user();
        if (user != null) {
            log.setUserId(user.userId());
            log.setUserEmail(user.email());
            log.setUserRole(user.role());
            log.setPortfolioId(user.portfolioId());
            return;
        }
        log.setUserEmail(truncate(sanitizer.field(log.getRequestBody(), "email"), 150));
        String userId = sanitizer.field(log.getResponseBody(), "userId");
        if (userId != null) log.setUserId(UUID.fromString(userId));
    }

    private static AuditLogChange toChange(AuditLog log, EntityChange change) {
        AuditLogChange row = new AuditLogChange();
        row.setId(UUID.randomUUID());
        row.setAuditLogId(log.getId());
        row.setPortfolioId(change.portfolioId() != null ? change.portfolioId() : log.getPortfolioId());
        row.setEntityName(change.entityName());
        row.setEntityId(change.entityId());
        row.setOperation(change.operation());
        row.setBeforeState(change.before());
        row.setAfterState(change.after());
        row.setChangedFields(change.changedFields());
        row.setCreatedAt(change.at());
        return row;
    }

    private static String moduleOf(String path) {
        String module = ApiModules.moduleForPath(path);
        return module != null ? module : ApiModules.firstSegment(path);
    }

    private static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }
}
