package com.skr.erp.common.constants;

// Shared rule for telling a sale's own checkout payment apart from later clearances / due settlements.
public final class PaymentRules {

    // A sale's own checkout payments are saved in the same transaction as the sale; anything later is a clearance.
    public static final long CHECKOUT_WINDOW_SECONDS = 5;

    // SQL form of the same rule, for customer_payment p joined to sales_order s (space-padded: text blocks trim trailing spaces).
    public static final String PAID_AFTER_CHECKOUT_SQL = " p.created_at > s.created_at + INTERVAL '5 seconds' ";

    private PaymentRules() {
    }
}
