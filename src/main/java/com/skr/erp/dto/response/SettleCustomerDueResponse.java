package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Result of a standalone due settlement: what was paid, what is still owed, and each invoice touched. */
@Data
@Builder
public class SettleCustomerDueResponse {

    private BigDecimal amountSettled;
    private BigDecimal remainingDue;
    private List<Allocation> allocations;

    @Data
    @Builder
    public static class Allocation {
        private String invoiceNumber;
        private BigDecimal amount;
        private BigDecimal invoiceDue;        // balance left on this invoice after the payment
        private PaymentStatus paymentStatus;  // PAID when fully cleared, else DUE
    }
}
