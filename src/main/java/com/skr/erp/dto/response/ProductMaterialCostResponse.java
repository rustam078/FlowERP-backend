package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ProductMaterialCostResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private BigDecimal cost;
    private BigDecimal salePrice;
    private LocalDate effectiveFrom;
    private String remarks;
    private LocalDateTime createdAt;
}