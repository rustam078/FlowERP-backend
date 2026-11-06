package com.skr.erp.exception;

import com.skr.erp.dto.response.ReconcileBatchInfo;
import lombok.Getter;

import java.util.List;

/**
 * Thrown when a scanned QR sale needs physical-stock reconciliation on one or more batches
 * (system stock is 0/short but the scanned QR units are still AVAILABLE). Carries the
 * per-batch details so the UI can confirm reconciliation batch-wise, then resend the sale
 * with those batch numbers in {@code reconcileBatches}.
 */
@Getter
public class ReconciliationRequiredException extends RuntimeException {

    private final transient List<ReconcileBatchInfo> batches;
    public ReconciliationRequiredException(List<ReconcileBatchInfo> batches) {
        super("Stock reconciliation required for scanned QR units.");
        this.batches = batches;
    }
}
