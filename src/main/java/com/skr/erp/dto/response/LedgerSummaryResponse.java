package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class LedgerSummaryResponse {
    private PaymentType paymentType;
    private BigDecimal totalEarned;   // piece-work total, or current-month payable salary
    private BigDecimal totalAdvance;
    private BigDecimal totalPayout;
    private BigDecimal netPayable;    // earned − advance − payout
    private List<LedgerEntryResponse> entries;
}
