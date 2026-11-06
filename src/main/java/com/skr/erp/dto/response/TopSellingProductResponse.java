package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class TopSellingProductResponse {

    private UUID productId;
    private String productName;
    private BigDecimal totalQuantitySold;
    private BigDecimal totalRevenue;
}
