package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class EmployeeProductionHistoryResponse {

    private LocalDate productionDate;
    private UUID productId;
    private String productName;
    private Integer quantity;
    private BigDecimal rate;
    private BigDecimal earnings;
}