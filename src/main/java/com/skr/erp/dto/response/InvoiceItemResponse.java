package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InvoiceItemResponse {

    private String productName;
    private BigDecimal quantity;
    private BigDecimal sellingPrice;
    private BigDecimal discount;
    private BigDecimal lineTotal;
}
