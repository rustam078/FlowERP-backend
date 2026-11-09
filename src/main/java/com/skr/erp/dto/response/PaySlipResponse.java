package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One month's pay-slip for a monthly-salary employee (earned is gross minus leave deduction).
 */
@Data
@Builder
public class PaySlipResponse {
    private LocalDate period;        // first day of the month
    private String monthLabel;       // e.g. "Nov 2026"
    private BigDecimal baseSalary;   // gross for the days employed in this month
    private double leaveDays;
    private BigDecimal leaveDeduction;
    private BigDecimal earned;       // baseSalary − leaveDeduction
    private BigDecimal paid;         // payouts + advance adjusted for this month
    private BigDecimal due;          // earned − paid
    private String status;           // UNPAID | PARTIAL | PAID
    private boolean current;         // in-progress month (accruing)
    private boolean payable;         // past month with a balance due (current/future are not payable)
}
