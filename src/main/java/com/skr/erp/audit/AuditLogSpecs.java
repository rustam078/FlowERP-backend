package com.skr.erp.audit;

import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Builds the audit log search from the optional filters (only filters that are set are applied). */
final class AuditLogSpecs {

    private AuditLogSpecs() {
    }

    static Specification<AuditLog> of(AuditLogFilter f) {
        List<Specification<AuditLog>> specs = new ArrayList<>();
        if (f.getFilterPortfolioId() != null) specs.add((r, q, cb) -> cb.equal(r.get("portfolioId"), f.getFilterPortfolioId()));
        if (hasText(f.getModule())) specs.add((r, q, cb) -> cb.equal(r.get("module"), f.getModule()));
        if (hasText(f.getOutcome())) specs.add(outcome(f.getOutcome()));
        if (f.getFromDate() != null) specs.add((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("createdAt"), f.getFromDate().atStartOfDay()));
        if (f.getToDate() != null) specs.add((r, q, cb) -> cb.lessThan(r.get("createdAt"), f.getToDate().plusDays(1).atStartOfDay()));
        if (hasText(f.getSearch())) specs.add(search(f.getSearch()));
        if (f.getEntityId() != null) specs.add(changedEntity(f.getEntityId()));
        return Specification.allOf(specs);
    }

    private static Specification<AuditLog> outcome(String outcome) {
        boolean failed = "FAILED".equalsIgnoreCase(outcome);
        return (r, q, cb) -> failed ? cb.greaterThanOrEqualTo(r.get("responseStatus"), 400)
                : cb.lessThan(r.get("responseStatus"), 400);
    }

    // A "Ref" (first 8 hex chars of the request id) matches that request; any text matches action / URL / email.
    private static Specification<AuditLog> search(String text) {
        String term = text.trim().toLowerCase(Locale.ROOT);
        if (term.matches("[0-9a-f]{8}")) return refRange(term);
        String contains = "%" + term + "%";
        return (r, q, cb) -> cb.or(
                cb.like(cb.lower(r.get("action")), contains),
                cb.like(cb.lower(r.get("urlPath")), contains),
                cb.like(cb.lower(r.get("userEmail")), contains));
    }

    // Every request id starting with the Ref lies in this id range (uses the unique index, no casting).
    private static Specification<AuditLog> refRange(String ref) {
        UUID low = UUID.fromString(ref + "-0000-0000-0000-000000000000");
        UUID high = UUID.fromString(ref + "-ffff-ffff-ffff-ffffffffffff");
        return (r, q, cb) -> cb.between(r.get("requestId"), low, high);
    }

    private static Specification<AuditLog> changedEntity(UUID entityId) {
        return (r, q, cb) -> {
            Subquery<UUID> changed = q.subquery(UUID.class);
            var change = changed.from(AuditLogChange.class);
            changed.select(change.get("auditLogId")).where(cb.equal(change.get("entityId"), entityId));
            return r.get("id").in(changed);
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
