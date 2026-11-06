package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ProductionItemResponse {

    private String work;
    private UUID productId;
    private String productName;
    private Integer quantity;
    private BigDecimal rate;
    private BigDecimal amount;
}
