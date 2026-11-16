package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** One FIFO-available batch for the sale form's live price preview and batch picker. */
@Data
@Builder
public class SaleBatchResponse {

    private UUID batchId;
    private String batchNumber;
    private LocalDate receivedDate;
    private BigDecimal quantityAvailable;
    private BigDecimal mrp;          // the lot's own printed price (null when the lot has none)
    private BigDecimal sellingPrice; // price actually charged: the lot's price, else the product's
    private BigDecimal unitCost;
}
