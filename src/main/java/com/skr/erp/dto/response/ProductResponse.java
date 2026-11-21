package com.skr.erp.dto.response;

import com.skr.erp.common.constants.ProductSource;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@Data
public class ProductResponse {

    private UUID id;
    private String name;
    private ProductSource source;
    private String iconName;
    private String description;
    private Boolean active;
    private Integer totalPieceCodes;
    private Integer activePieceCodes;
    private Integer inactivePieceCodes;
    // Price the next sale will use: the oldest in-stock batch's price (FIFO); base price when out of stock.
    private BigDecimal sellingPrice;
    // Total in-stock quantity across batches (for the product card).
    private Integer availableQty;
    // In-stock prices in selling order with quantities (card tooltip: what applies after the current stock runs out).
    private List<StockPriceResponse> stockPrices;
    // True when the product has an uploaded image (fetch it at /api/products/{id}/image).
    private Boolean hasImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}