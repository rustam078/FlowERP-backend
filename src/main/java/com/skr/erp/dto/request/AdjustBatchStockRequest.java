package com.skr.erp.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class AdjustBatchStockRequest {
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
    private BigDecimal quantity;
    @NotBlank(message = "Direction is required (INCREASE or DECREASE)")
    private String direction;
    private String remarks;
}
