package com.skr.erp.dto.response;

import com.skr.erp.common.constants.InvestmentType;
import com.skr.erp.common.constants.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InvestmentDetailsResponse {

    private UUID id;
    private String referenceNumber;
    private String invoiceNumber;
    private UUID vendorId;
    private String vendorName;
    private InvestmentType investmentType;
    private LocalDate purchaseDate;
    private String remarks;
    private BigDecimal subTotal;
    private BigDecimal gstAmount;
    private BigDecimal discountAmount;
    private BigDecimal otherCharge;
    private BigDecimal grandTotal;

    // Payment tracking
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private PaymentStatus paymentStatus;
    private List<InvestmentPaymentResponse> payments;
    private List<InvestmentItemResponse> items;
}