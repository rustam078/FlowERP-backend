package com.skr.erp.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

// One product produced in a Produce request. Cost/selling optional → fall back to the product's base.
@Data
public class ProduceItemRequest {
    @NotNull(message = "Product is required")
    private UUID productId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    private BigDecimal unitCost;      // making cost per piece for this lot
    private BigDecimal sellingPrice;  // selling price per piece for this lot
}
