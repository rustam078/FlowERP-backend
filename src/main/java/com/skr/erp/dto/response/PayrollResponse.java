package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full payroll account for an employee: overview totals + monthly slips + transaction log.
 */
@Data
@Builder
public class PayrollResponse {
    private PaymentType paymentType;
    private BigDecimal monthlySalary;     // configured full salary (for the Salary column)
    private BigDecimal totalEarned;       // Σ slip earned (monthly) or piece total (piece-rate)
    private BigDecimal totalAdvanceGiven; // Σ advances ever given
    private BigDecimal advanceBalance;    // advance not yet applied to any slip
    private BigDecimal totalPaid;         // Σ settlement payouts
    private BigDecimal pending;           // earned − advances − payouts (negative = paid ahead)
    private double leaveDaysThisMonth;
    private List<PaySlipResponse> slips;          // one page; empty for piece-rate
    private long slipTotal;
    private List<LedgerEntryResponse> transactions; // one page (filtered)
    private long txnTotal;
}
