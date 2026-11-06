package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class RecentSaleResponse {

    private String invoiceNo;
    private String customerName;
    private BigDecimal grandTotal;
    private PaymentStatus paymentStatus;
    private LocalDate saleDate;
}
