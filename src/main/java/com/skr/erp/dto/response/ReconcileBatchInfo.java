package com.skr.erp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One batch whose system stock is short of the scanned QR quantity — surfaced to the UI
 * so the user can approve a physical-stock reconciliation (batch-wise, one prompt per batch).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReconcileBatchInfo {
    private String batchNumber;
    private String productName;
    private BigDecimal available;   // current system stock
    private BigDecimal requested;   // scanned QR units for this batch
    private BigDecimal deficit;     // requested - available (to reconcile)
}
