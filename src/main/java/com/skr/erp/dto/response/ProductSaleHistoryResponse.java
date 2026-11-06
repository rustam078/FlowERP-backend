package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ProductSaleHistoryResponse {
    private String invoiceNo;
    private LocalDate saleDate;
    private String customerName;
    private BigDecimal quantitySold;
    private BigDecimal sellingPrice;
    private BigDecimal profit;
}
