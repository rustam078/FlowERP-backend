package com.skr.erp.dto.request;

import com.skr.erp.common.constants.InvestmentItemType;
import com.skr.erp.common.constants.UnitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateInvestmentItemRequest {

    @NotNull
    private InvestmentItemType itemType;
    @NotBlank
    private String itemName;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal quantity;
    @NotNull
    private UnitType unit;
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal rate;

    // Optional MRP / selling price for this lot (products only).
    private BigDecimal sellingPrice;
}