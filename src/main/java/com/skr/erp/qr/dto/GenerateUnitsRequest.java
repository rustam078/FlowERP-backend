package com.skr.erp.qr.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class GenerateUnitsRequest {

    /**
     * How many new units (labels) to create. When null, defaults to the number of
     * pieces in the batch that are not yet labelled (quantityReceived - existing units).
     */
    @Min(1)
    private Integer count;
}
