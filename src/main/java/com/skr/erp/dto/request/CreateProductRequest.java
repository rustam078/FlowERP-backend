package com.skr.erp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;
    private String iconName; // optional
    private String description;

    // Optional piece codes (legacy). Products created from the Product page carry none.
    @Valid
    private List<CreatePieceCodeRequest> pieceCodes;

    // Costing + selling price; selling price becomes effective from today in the pricing history.
    @NotNull(message = "Cost price is required")
    @DecimalMin("0.00")
    private BigDecimal cost;
    @NotNull(message = "Selling price is required")
    @DecimalMin("0.00")
    private BigDecimal sellingPrice;
}