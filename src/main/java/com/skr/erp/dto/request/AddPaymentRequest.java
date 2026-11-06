package com.skr.erp.dto.request;

import com.skr.erp.common.constants.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AddPaymentRequest {

    @NotNull
    private LocalDate paymentDate;
    @NotNull
    private PaymentMode mode;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    private String reference;
}
