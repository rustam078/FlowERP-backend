package com.skr.erp.dto.request;

import lombok.Data;

import java.time.LocalDate;

/**
 * Filters for the payroll card: the salary-slip year and the paged transaction log.
 */
@Data
public class PayrollQuery {
    private Integer year;        // calendar year of the salary slips; null = current year
    private String txnType;      // ADVANCE | SPEND | SETTLEMENT | null (all)
    private LocalDate txnFrom;
    private LocalDate txnTo;
    private int txnPage;         // 0-based
    private int txnSize = 5;
}
