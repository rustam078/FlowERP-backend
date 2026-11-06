package com.skr.erp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Aggregate totals for the sales list header, respecting the active filters. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SalesSummaryResponse {
    private long count;
    private BigDecimal revenue;
    private BigDecimal discount;
    private BigDecimal received; // total amount actually collected
    private BigDecimal due;      // total still outstanding (revenue − received)
}
