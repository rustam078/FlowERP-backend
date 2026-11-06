package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentMode;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** One customer payment and how it was split across invoices. */
@Getter
@Builder
public class CustomerPaymentGroupResponse {

    private LocalDate paymentDate;
    private PaymentMode mode;
    private BigDecimal totalAmount;
    private List<Allocation> allocations;

    @Getter
    @Builder
    public static class Allocation {
        private String invoiceNumber;
        private BigDecimal amount;
    }
}
