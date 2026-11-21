package com.skr.erp.audit;

import com.skr.erp.common.response.PageResponse;
import com.skr.erp.entity.Portfolio;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** SUPER_ADMIN audit log viewer: filtered, paged list and full detail (3 queries per page, no N+1). */
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;
    private final AuditLogChangeRepository auditLogChangeRepository;
    private final PortfolioRepository portfolioRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogSummaryResponse> search(AuditLogFilter filter, int page, int size) {
        if (filter.getFromDate() != null && filter.getToDate() != null && filter.getFromDate().isAfter(filter.getToDate())) {
            throw new BusinessException("fromDate cannot be after toDate");
        }
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> logs = auditLogRepository.findAll(AuditLogSpecs.of(filter), pageable);
        Map<UUID, Long> changeCounts = changeCounts(logs.getContent().stream().map(AuditLog::getId).toList());
        Map<UUID, String> names = portfolioNames();
        List<AuditLogSummaryResponse> rows = logs.getContent().stream()
                .map(l -> toSummary(l, names.get(l.getPortfolioId()), changeCounts.getOrDefault(l.getId(), 0L)))
                .toList();
        return PageResponse.<AuditLogSummaryResponse>builder()
                .content(rows).page(logs.getNumber()).size(logs.getSize())
                .totalElements(logs.getTotalElements()).totalPages(logs.getTotalPages()).last(logs.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public AuditLogDetailResponse detail(UUID id) {
        AuditLog log = auditLogRepository.findById(id).orElseThrow(() -> new BusinessException("Audit log not found"));
        String portfolioName = log.getPortfolioId() == null ? null
                : portfolioRepository.findById(log.getPortfolioId()).map(Portfolio::getName).orElse(null);
        return AuditLogDetailResponse.builder()
                .log(log).ref(AuditRequestFilter.shortId(log.getRequestId())).portfolioName(portfolioName)
                .changes(auditLogChangeRepository.findByAuditLogIdOrderByCreatedAtAsc(id))
                .build();
    }

    private Map<UUID, Long> changeCounts(List<UUID> ids) {
        Map<UUID, Long> counts = new HashMap<>();
        if (ids.isEmpty()) return counts;
        for (Object[] row : auditLogChangeRepository.countByAuditLogIds(ids)) counts.put((UUID) row[0], ((Number) row[1]).longValue());
        return counts;
    }

    // Portfolios are few, so one read maps ids to names for the whole page.
    private Map<UUID, String> portfolioNames() {
        return portfolioRepository.findAll().stream().collect(Collectors.toMap(Portfolio::getId, Portfolio::getName));
    }

    private static AuditLogSummaryResponse toSummary(AuditLog l, String portfolioName, long changeCount) {
        return AuditLogSummaryResponse.builder()
                .id(l.getId()).ref(AuditRequestFilter.shortId(l.getRequestId())).createdAt(l.getCreatedAt())
                .portfolioId(l.getPortfolioId()).portfolioName(portfolioName)
                .userEmail(l.getUserEmail()).userRole(l.getUserRole())
                .action(l.getAction()).module(l.getModule()).httpMethod(l.getHttpMethod()).urlPath(l.getUrlPath())
                .responseStatus(l.getResponseStatus()).durationMs(l.getDurationMs()).errorMessage(l.getErrorMessage())
                .changeCount(changeCount)
                .build();
    }
}
