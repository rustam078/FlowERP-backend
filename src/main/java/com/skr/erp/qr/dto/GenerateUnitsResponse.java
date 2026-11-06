package com.skr.erp.qr.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GenerateUnitsResponse {

    private String batchNumber;
    private String productName;

    /** Units created by this call (the ones you need to print now). */
    private int generatedCount;

    /** Total units that now exist for the batch. */
    private long totalUnits;

    private List<ProductUnitResponse> units;
}
