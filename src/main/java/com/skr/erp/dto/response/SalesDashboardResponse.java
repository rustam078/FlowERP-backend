package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SalesDashboardResponse {

    private long todaySales;
    private BigDecimal todayRevenue;
    private long monthSales;
    private BigDecimal monthRevenue;
    private long totalSales;
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;
}
