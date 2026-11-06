package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** Aggregate production KPIs for the summary cards (so the list can paginate server-side). */
@Getter
@Builder
public class ProductionStatsResponse {

    private long totalEntries;
    private long totalQuantity;
    private BigDecimal totalAmount;
    private long todayQuantity;
    private BigDecimal todayAmount;
    private long monthQuantity;
    private BigDecimal monthAmount;
}
