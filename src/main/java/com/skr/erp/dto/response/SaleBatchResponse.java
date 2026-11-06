package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** One FIFO-available batch for the sale form's live price preview. */
@Data
@Builder
public class SaleBatchResponse {

    private String batchNumber;
    private BigDecimal quantityAvailable;
    private BigDecimal sellingPrice;
    private BigDecimal unitCost;
}
