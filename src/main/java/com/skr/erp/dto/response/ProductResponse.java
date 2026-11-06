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
    // Current effective selling price (null when none configured).
    private BigDecimal sellingPrice;
    // Total in-stock quantity across batches (for the product card).
    private Integer availableQty;
    // Distinct in-stock batch selling prices (for the card price tooltip when they vary).
    private List<BigDecimal> batchSalePrices;
    // True when the product has an uploaded image (fetch it at /api/products/{id}/image).
    private Boolean hasImage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}