package com.skr.erp.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateMaterialCostRequest {

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal cost;
    @DecimalMin("0.00")
    private BigDecimal salePrice;
    @NotNull
    private LocalDate effectiveFrom;

}