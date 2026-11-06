package com.skr.erp.common.constants;

public enum PaymentStatus {
    PAID,
    DUE,   // still owes money (was CREDIT — renamed to avoid clashing with PaymentMode.CREDIT)
    PENDING,
    PARTIALLY_PAID,
    FAILED,
    REFUNDED
}