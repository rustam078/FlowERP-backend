package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductionDetailsResponse {

    private UUID id;
    private String employeeName;
    private LocalDate productionDate;
    private Integer productCount;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private List<ProductionItemResponse> items;
}