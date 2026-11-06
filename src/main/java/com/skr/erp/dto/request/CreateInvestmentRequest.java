package com.skr.erp.dto.request;

import com.skr.erp.common.constants.InvestmentType;
import com.skr.erp.common.constants.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateInvestmentRequest {

    private UUID vendorId;
    @NotNull
    private InvestmentType investmentType;
    @NotNull
    private LocalDate purchaseDate;
    private String invoiceNumber;
    private BigDecimal gstAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal otherCharge = BigDecimal.ZERO;
    @Valid
    @NotEmpty
    private List<CreateInvestmentItemRequest> items;

    // ── Optional payment recorded at creation time ──
    // fullPayment=true pays the whole grand total; otherwise paymentAmount (if > 0) is used.
    private Boolean fullPayment;
    private BigDecimal paymentAmount;
    private PaymentMode paymentMode;
    private LocalDate paymentDate;
    private String paymentReference;
}