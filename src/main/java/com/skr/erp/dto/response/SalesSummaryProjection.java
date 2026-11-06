package com.skr.erp.dto.response;

import java.math.BigDecimal;

public interface SalesSummaryProjection {
    Long getTotalSales();
    BigDecimal getTotalRevenue();

}