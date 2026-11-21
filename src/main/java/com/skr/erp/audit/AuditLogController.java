package com.skr.erp.audit;

import com.skr.erp.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// Audit log viewer. SUPER_ADMIN only (enforced in PermissionInterceptor).
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    // Newest first; filters: filterPortfolioId, module, outcome, fromDate, toDate, search, entityId.
    @GetMapping
    public ResponseEntity<PageResponse<AuditLogSummaryResponse>> search(@ModelAttribute AuditLogFilter filter,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogQueryService.search(filter, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLogDetailResponse> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(auditLogQueryService.detail(id));
    }
}
