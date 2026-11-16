package com.skr.erp.dto.response;

import com.skr.erp.common.response.PageResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** A page of Revenue transactions plus filter-aware totals (money in, money out, net). */
@Data
@Builder
public class RevenueResponse {
    private PageResponse<RevenueTransactionResponse> transactions;
    private BigDecimal inflow;
    private BigDecimal outflow;
    private BigDecimal net;
}
