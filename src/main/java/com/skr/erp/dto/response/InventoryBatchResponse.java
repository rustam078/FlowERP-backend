package com.skr.erp.dto.response;

import com.skr.erp.common.constants.ProductSource;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class InventoryBatchResponse {

    private UUID batchId;
    private String batchNumber;
    private ProductSource source;
    private LocalDate receivedDate;
    private BigDecimal totalQuantity;
    private BigDecimal quantityAvailable;
    private BigDecimal unitCost;
    private BigDecimal sellingPrice;
    private BigDecimal batchValue;
    private String status;
}