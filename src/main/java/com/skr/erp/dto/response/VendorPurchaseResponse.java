package com.skr.erp.dto.response;

import com.skr.erp.common.constants.InvestmentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@Data
public class VendorPurchaseResponse {

    private String invoiceNumber;
    private InvestmentType investmentType;
    private LocalDate purchaseDate;
    private BigDecimal grandTotal;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private String paymentStatus; // PENDING | PARTIALLY_PAID | PAID
    private List<VendorPurchaseItemResponse> items;
}