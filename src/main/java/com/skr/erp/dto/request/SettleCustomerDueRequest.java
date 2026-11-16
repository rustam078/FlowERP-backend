package com.skr.erp.dto.request;

import com.skr.erp.common.constants.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Settle a customer's outstanding dues without a new sale. The amount is allocated to unpaid
 * invoices oldest first; requestId makes a repeated submission (double click / retry) a no-op.
 */
@Data
public class SettleCustomerDueRequest {

    @NotBlank
    private String customerMobile;

    @NotNull
    @DecimalMin(value = "0.01", message = "Settlement amount must be greater than zero")
    private BigDecimal amount;

    @NotNull
    private PaymentMode paymentMode;


    @NotNull
    private UUID requestId;
}
