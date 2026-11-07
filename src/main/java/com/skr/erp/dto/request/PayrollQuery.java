package com.skr.erp.dto.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * Paging + filtering for the payroll card (slips and the transaction log are paged separately).
 */
@Data
public class PayrollQuery {
    private String txnType;      // ADVANCE | SPEND | SETTLEMENT | null (all)
    private LocalDate txnFrom;
    private LocalDate txnTo;
    private int txnPage;         // 0-based
    private int txnSize = 5;
    private int slipPage;
    private int slipSize = 10;
}
