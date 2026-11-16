package com.skr.erp.service;

import com.skr.erp.common.constants.RevenueModule;
import com.skr.erp.common.response.PageResponse;
import com.skr.erp.dto.request.RevenueFilter;
import com.skr.erp.dto.response.RevenueResponse;
import com.skr.erp.dto.response.RevenueTransactionResponse;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.RevenueTransactionRepository;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Revenue: one paged, filterable list of every money movement across Investment, Sales and Employees. */
@Service
@RequiredArgsConstructor
public class RevenueService {

    private static final int MAX_PAGE_SIZE = 100;

    // Transaction types each module can be filtered by.
    private static final Map<RevenueModule, Set<String>> TYPES = Map.of(
            RevenueModule.SALES, Set.of("SALE_PAYMENT", "DUE_CLEARED", "DUE_SETTLEMENT"),
            RevenueModule.INVESTMENT, Set.of("PURCHASE", "OVERHEAD"),
            RevenueModule.EMPLOYEE, Set.of("ADVANCE", "PAYOUT", "SPEND"));

    private final RevenueTransactionRepository revenueTransactionRepository;

    // A page of transactions plus totals for the same filter (2 queries, whatever the page size).
    @Transactional(readOnly = true)
    public RevenueResponse getTransactions(RevenueFilter filter, int page, int size) {
        validate(filter);
        int pageNo = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Tuple totals = revenueTransactionRepository.totals(filter);
        long count = totals.get("cnt", Number.class).longValue();
        List<RevenueTransactionResponse> rows = count == 0 ? List.of()
                : revenueTransactionRepository.findPage(filter, pageSize, (long) pageNo * pageSize)
                        .stream().map(RevenueService::toResponse).toList();
        BigDecimal inflow = totals.get("inflow", BigDecimal.class);
        BigDecimal outflow = totals.get("outflow", BigDecimal.class);
        return RevenueResponse.builder()
                .transactions(pageOf(rows, pageNo, pageSize, count))
                .inflow(inflow).outflow(outflow).net(inflow.subtract(outflow))
                .build();
    }

    private void validate(RevenueFilter filter) {
        if (filter.getFromDate() != null && filter.getToDate() != null && filter.getFromDate().isAfter(filter.getToDate())) {
            throw new BusinessException("fromDate cannot be after toDate");
        }
        String type = filter.getType();
        if (type == null || type.isBlank()) return;
        boolean known = filter.getModule() != null
                ? TYPES.get(filter.getModule()).contains(type)
                : TYPES.values().stream().anyMatch(types -> types.contains(type));
        if (!known) throw new BusinessException("Unknown transaction type: " + type);
    }

    private static PageResponse<RevenueTransactionResponse> pageOf(List<RevenueTransactionResponse> rows, int page, int size, long count) {
        int totalPages = (int) ((count + size - 1) / size);
        return PageResponse.<RevenueTransactionResponse>builder()
                .content(rows).page(page).size(size)
                .totalElements(count).totalPages(totalPages)
                .last(page + 1 >= totalPages)
                .build();
    }

    private static RevenueTransactionResponse toResponse(Tuple t) {
        return RevenueTransactionResponse.builder()
                .id(toUuid(t.get("id")))
                .module(RevenueModule.valueOf(t.get("module", String.class)))
                .type(t.get("type", String.class))
                .date(toDate(t.get("txn_date")))
                .reference(t.get("reference", String.class))
                .note(t.get("note", String.class))
                .party(t.get("party", String.class))
                .contact(t.get("contact", String.class))
                .paymentMode(t.get("mode", String.class))
                .amount(t.get("amount", BigDecimal.class))
                .direction(t.get("direction", String.class))
                .sourceId(toUuid(t.get("source_id")))
                .build();
    }

    private static UUID toUuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(String.valueOf(value));
    }

    private static LocalDate toDate(Object value) {
        return value instanceof Date sqlDate ? sqlDate.toLocalDate() : (LocalDate) value;
    }
}
