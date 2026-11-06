package com.skr.erp.dto.response;

import com.skr.erp.dto.response.ProductionItemResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductionResponse {

    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private LocalDate productionDate;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private Integer productCount;
    private List<ProductionItemResponse> items;
}