package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {

    private String invoiceNumber;
    private LocalDate date;
    private String customerName;
    private String customerMobile;
    private String customerEmail;
    private PaymentMode paymentMode;
    private String paymentProvider;
    private PaymentStatus paymentStatus;
    private List<InvoiceItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal grandTotal;
    private BigDecimal amountReceived;       // total collected in this transaction
    private BigDecimal paidToPreviousDues;   // part applied to older invoices
    private BigDecimal balanceDue;           // this invoice's own remaining due (hidden once cleared)
    private List<SalesDetailsResponse.Clearance> clearances; // later payments that cleared this invoice
    private List<SalesDetailsResponse.PreviousDue> previousDuesAllocation; // older invoices this checkout cleared
}
