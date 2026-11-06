package com.skr.erp.qr;

import com.skr.erp.common.response.PageResponse;
import com.skr.erp.qr.dto.BatchLabelSummary;
import com.skr.erp.qr.dto.GenerateUnitsRequest;
import com.skr.erp.qr.dto.GenerateUnitsResponse;
import com.skr.erp.qr.dto.ProductUnitResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface QrUnitService {

    GenerateUnitsResponse generateUnits(String batchNumber, GenerateUnitsRequest request);
    BatchLabelSummary summary(String batchNumber);
    List<ProductUnitResponse> listUnits(String batchNumber);
    PageResponse<ProductUnitResponse> listUnits(String batchNumber, Pageable pageable);
    PageResponse<ProductUnitResponse> searchUnits(String batchNumber, String serial, Pageable pageable);
    /** Existing labels whose serial index is in [from, to] — to print/reprint a specific chunk. */
    List<ProductUnitResponse> rangeUnits(String batchNumber, int from, int to);
    ProductUnitResponse scanLookup(String code);
    ProductUnitResponse voidUnit(String serial);
    /**
     * Mark scanned units SOLD as part of a sale. Validates each serial belongs to the
     * batch and is still AVAILABLE. Called by the sales flow; does not touch batch qty.
     */
    void consumeForSale(String batchNumber, List<String> serials, UUID saleOrderId);

    /**
     * Validate scanned serials for a sale WITHOUT marking them SOLD: each must exist, belong to
     * the given batch and be AVAILABLE. Throws otherwise (so a used/void/unknown tag fails hard
     * and is never offered for reconciliation).
     */
    void validateUnitsForSale(String batchNumber, List<String> serials);

    /**
     * Remove up to {@code count} AVAILABLE labels from a batch (newest first) when its stock is
     * reduced, so the QR labels stay in step with the stock. SOLD/VOID units are never removed.
     * Returns how many were actually removed.
     */
    int removeAvailableUnits(String batchNumber, int count);
}
