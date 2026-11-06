package com.skr.erp.dto.response;

import com.skr.erp.common.constants.ProductSource;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProductInventoryDetailResponse {

    private UUID productId;
    private String productName;
    private ProductSource source;
    private BigDecimal totalQuantity;
    private BigDecimal totalSold;
    private BigDecimal quantityAvailable;
    private BigDecimal totalValue;
    private BigDecimal averageCost;
    private List<InventoryBatchResponse> batches;

    private long totalBatches;
    private int batchPage;
    private int batchPageSize;
    private int batchTotalPages;
    private boolean batchLast;
}