package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class SalesListResponse {

    private UUID saleId;
    private String invoiceNo;
    private String customerName;
    private String customerMobile;
    private BigDecimal grandTotal;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private BigDecimal amountReceived; // total collected in this sale's transaction (incl. old dues cleared)
    private PaymentMode paymentMode;
    private PaymentStatus paymentStatus;
    private LocalDate saleDate;
}
