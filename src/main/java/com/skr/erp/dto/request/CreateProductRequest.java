package com.skr.erp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;
    @NotBlank(message = "Product icon is required")
    private String iconName;
    private String description;

    // One or more piece codes (each code + rate), all created with the product.
    @NotEmpty(message = "At least one piece code is required")
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