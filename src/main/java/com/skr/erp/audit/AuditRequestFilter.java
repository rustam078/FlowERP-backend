package com.skr.erp.audit;

import com.skr.erp.security.CurrentUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Outermost filter. Gives every request an id (response header X-Request-Id, and in every log line
 * via MDC), and for write calls to @Audit endpoints records request, response and row changes —
 * also when the call failed. The audit row is written after the response has been sent.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AuditRequestFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_KEY = "requestId";
    public static final String CURRENT_USER_ATTRIBUTE = "audit.currentUser";
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final int BODY_CACHE_LIMIT = 64 * 1024 + 1;
    private static final Logger log = LoggerFactory.getLogger(AuditRequestFilter.class);

    private final AuditRecorder auditRecorder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        UUID requestId = UUID.randomUUID();
        MDC.put(REQUEST_ID_KEY, shortId(requestId));
        response.setHeader("X-Request-Id", requestId.toString());
        try {
            if (WRITE_METHODS.contains(request.getMethod()) && request.getRequestURI().startsWith("/api/")) {
                auditedChain(request, response, chain, requestId);
            } else {
                chain.doFilter(request, response);
            }
        } finally {
            MDC.remove(REQUEST_ID_KEY);
        }
    }

    // Short form of a request id: shown in logs and as "Ref" in error messages; searchable in the audit log.
    public static String shortId(UUID requestId) {
        return requestId.toString().substring(0, 8);
    }

    private void auditedChain(HttpServletRequest request, HttpServletResponse response, FilterChain chain, UUID requestId)
            throws ServletException, IOException {
        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request, BODY_CACHE_LIMIT);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);
        Started started = new Started(requestId, LocalDateTime.now(), System.nanoTime());
        AuditChangeCollector.start();
        try {
            chain.doFilter(req, res);
        } finally {
            List<EntityChange> changes = AuditChangeCollector.finish();
            Result result = new Result(res.getStatus(), res.getContentAsByteArray(), changes);
            res.copyBodyToResponse(); // the user gets the response before the audit row is written
            recordIfAudited(req, started, result);
        }
    }

    private void recordIfAudited(ContentCachingRequestWrapper req, Started started, Result result) {
        Audit audit = auditAnnotation(req);
        if (audit == null) return;
        long durationMs = (System.nanoTime() - started.nanos()) / 1_000_000;
        recordSafely(new AuditCapture(started.requestId(), audit.value(), req.getMethod(), req.getRequestURI(),
                (String) req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE), req.getParameterMap(),
                req.getContentAsByteArray(), req.getContentType(), result.status(), result.body(), started.at(),
                durationMs, (CurrentUser) req.getAttribute(CURRENT_USER_ATTRIBUTE), clientIp(req),
                req.getHeader("User-Agent"), result.changes()));
    }

    private record Started(UUID requestId, LocalDateTime at, long nanos) {
    }

    private record Result(int status, byte[] body, List<EntityChange> changes) {
    }

    // An audit failure is logged, never shown to the user or allowed to break their request.
    private void recordSafely(AuditCapture capture) {
        try {
            auditRecorder.record(capture);
        } catch (Exception e) {
            log.warn("Audit write failed for request {}", capture.requestId(), e);
        }
    }

    private static Audit auditAnnotation(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        return handler instanceof HandlerMethod method ? method.getMethodAnnotation(Audit.class) : null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
