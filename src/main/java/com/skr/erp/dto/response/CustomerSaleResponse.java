package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CustomerSaleResponse {

    private String invoiceNo;
    private LocalDate saleDate;
    private BigDecimal grandTotal;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private PaymentStatus paymentStatus;
}
