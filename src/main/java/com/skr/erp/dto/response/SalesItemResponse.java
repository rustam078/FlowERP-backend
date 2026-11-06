package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class SalesItemResponse {

    private UUID productId;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal sellingPrice;
    private BigDecimal unitCost;
    private BigDecimal discount;
    private BigDecimal lineTotal;
    private BigDecimal profit;
}
