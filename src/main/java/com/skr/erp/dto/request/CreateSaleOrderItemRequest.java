package com.skr.erp.dto.request;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateSaleOrderItemRequest {

    @NotNull
    private UUID productId;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal quantity;
    /** Scanned lines: frozen printed price. Typed lines: ignored (FIFO batch price used). */
    @DecimalMin("0.00")
    private BigDecimal sellingPrice;
    /** Deprecated: discount is now order-level; kept for request compatibility. */
    @DecimalMin("0.00")
    private BigDecimal discount;
    private String batchNumber;

    /** Scanned unit serials backing this line (size should equal quantity). */
    private List<String> serials;
}