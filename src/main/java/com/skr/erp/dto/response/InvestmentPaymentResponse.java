package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentMode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InvestmentPaymentResponse {

    private UUID id;
    private LocalDate paymentDate;
    private PaymentMode mode;
    private BigDecimal amount;
    private String reference;
    private LocalDateTime createdAt;

    // Set only on the invoice the payment was made from (origin), when split across invoices.
    private boolean groupOrigin;
    private BigDecimal groupTotal;
    private List<Allocation> allocations;

    // Set on the OTHER invoices a split payment cleared — names the invoice it was paid from.
    private String clearedFromInvoice;

    // How much of a single payment landed on each invoice (oldest first).
    @Getter
    @Builder
    public static class Allocation {
        private String invoiceNumber;
        private BigDecimal amount;
        private boolean thisInvoice;
    }
}
