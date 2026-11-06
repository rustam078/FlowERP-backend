package com.skr.erp.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePieceCodeRequest {

    @NotBlank
    private String code;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal rate;
}