package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SalesDetailsResponse {

    private UUID saleId;
    private String invoiceNo;
    private LocalDate saleDate;
    private String customerName;
    private String customerMobile;
    private String customerEmail;
    private PaymentMode paymentMode;
    private String paymentProvider;
    private PaymentStatus paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal grandTotal;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private BigDecimal amountReceived;       // total collected in this transaction (this bill + old dues)
    private BigDecimal paidToPreviousDues;   // part of the payment applied to older invoices
    private BigDecimal customerBalanceDue;   // customer's remaining balance across all sales
    private BigDecimal totalProfit;
    private List<SalesItemResponse> items;
    private List<Clearance> clearances;  // payments that cleared this invoice from a later checkout
    private List<PreviousDue> previousDuesAllocation; // older invoices this checkout's payment cleared

    /** One cross-invoice clearance: when, by which mode, how much, and the clearing invoice. */
    @Data
    @Builder
    public static class Clearance {
        private LocalDate date;
        private PaymentMode mode;
        private BigDecimal amount;
        private String referenceInvoiceNo;
    }

    /** One older invoice cleared by this checkout's payment (invoice no + amount applied). */
    @Data
    @Builder
    public static class PreviousDue {
        private String invoiceNumber;
        private BigDecimal amount;
    }
}
