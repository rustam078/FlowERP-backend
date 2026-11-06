package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** Per-source AVAILABLE-stock totals for the product detail cards (purchase + production). */
@Data
@Builder
public class ProductStockSummaryResponse {
    private BigDecimal purchasedQty;    // available purchased
    private BigDecimal purchasedCost;
    private BigDecimal manufacturedQty; // available produced
    private BigDecimal manufacturedCost;
    private BigDecimal totalQty;        // available: purchasedQty + manufacturedQty
    private BigDecimal totalCost;       // available: purchasedCost + manufacturedCost
    private BigDecimal stockValue;      // Σ availableQty × selling price
    private BigDecimal soldQty;         // received − available, across all batches
}
