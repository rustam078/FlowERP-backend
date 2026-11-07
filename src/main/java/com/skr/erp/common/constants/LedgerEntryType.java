package com.skr.erp.common.constants;

// Payroll account movements. ADVANCE = money given ahead; PAYOUT = slip settlement paid;
// ADVANCE_ADJUSTMENT = advance auto-applied to a month's slip at month end. (EARNING_SALARY: legacy.)
public enum LedgerEntryType {
    EARNING_SALARY,
    ADVANCE,
    PAYOUT,
    ADVANCE_ADJUSTMENT,
    SPEND          // weekly pocket expense; counts like an advance but shows a separate tag
}
