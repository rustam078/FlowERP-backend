package com.skr.erp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Turn a PURCHASED product into BOTH by adding its manufacturing piece codes + pricing. */
@Data
public class EnableManufacturingRequest {

    @NotEmpty(message = "At least one piece code is required")
    @Valid
    private List<CreatePieceCodeRequest> pieceCodes;

    @NotNull(message = "Cost price is required")
    @DecimalMin("0.00")
    private BigDecimal cost;
    @NotNull(message = "Selling price is required")
    @DecimalMin("0.00")
    private BigDecimal sellingPrice;
}
