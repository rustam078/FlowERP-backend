package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class EmployeeProductSummaryResponse {

    private UUID productId;
    private String productName;
    private Integer quantity;
    private BigDecimal earnings;
}