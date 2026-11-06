package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EmployeeSummaryResponse {

    private Integer totalProductionQty;
    private BigDecimal totalEarnings;
    private BigDecimal currentMonthEarnings;
    private Integer productsWorkedOn;
}