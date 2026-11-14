package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One month's pay-slip for a monthly-salary employee (earned is gross minus leave deduction).
 * INACTIVE months (no working day) carry zero amounts and no payment status.
 */
@Data
@Builder
public class PaySlipResponse {
    private LocalDate period;        // first day of the month
    private String monthLabel;       // e.g. "Nov 2026"
    private String monthStatus;      // ACTIVE | INACTIVE
    private int activeDays;          // working days counted in this month
    private int monthDays;           // calendar days in this month
    private BigDecimal monthlySalary; // salary in force for this month (saved snapshot)
    private BigDecimal baseSalary;   // gross for the active days of this month
    private double leaveDays;        // leave on active days only
    private BigDecimal leaveDeduction;
    private BigDecimal earned;       // baseSalary − leaveDeduction
    private BigDecimal paid;         // payouts + advance adjusted for this month
    private BigDecimal due;          // earned − paid
    private String status;           // UNPAID | PARTIAL | PAID; null when nothing is payable
    private boolean current;         // in-progress month (accruing, calculated live)
    private boolean payable;         // past active month with a balance due
    private boolean finalized;       // fully paid and locked
}
