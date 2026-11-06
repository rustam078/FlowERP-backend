package com.skr.erp.dto.response;

import com.skr.erp.common.constants.ProductSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockAlertResponse {

    private UUID productId;
    private String productName;
    private ProductSource source;
    private BigDecimal currentStock;
    private BigDecimal threshold;
}