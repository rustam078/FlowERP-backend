package com.skr.erp.dto.response;

import com.skr.erp.common.constants.ProductSource;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Data
public class InventoryResponse {

    private UUID productId;
    private String productName;
    private ProductSource source;
    private BigDecimal currentStock;
    private BigDecimal stockValue;
    private BigDecimal averageCost;
    private BigDecimal sellingPrice;

}