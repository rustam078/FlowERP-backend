package com.skr.erp.dto.response;

import com.skr.erp.common.constants.InvestmentType;
import com.skr.erp.common.constants.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class InvestmentResponse {

    private UUID id;
    private String referenceNumber;
    private String invoiceNumber;
    private UUID vendorId;
    private String vendorName;
    private InvestmentType investmentType;
    private LocalDate purchaseDate;
    private LocalDateTime createdAt; // for sorting same-date rows by latest first
    private Integer itemCount;
    private BigDecimal grandTotal;
    private BigDecimal amountPaid;
    // Actual cash received in this invoice's own transaction (may clear previous dues too).
    private BigDecimal amountReceived;
    private PaymentStatus paymentStatus;
}