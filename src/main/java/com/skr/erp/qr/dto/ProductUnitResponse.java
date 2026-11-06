package com.skr.erp.qr.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class ProductUnitResponse {

    private UUID id;
    private String serial;
    private String batchNumber;
    private UUID productId;
    private String productName;
    private BigDecimal printedPrice;
    // The unit's batch cost (populated on scan lookup) so the sale form can show profit.
    private BigDecimal unitCost;
    private String status;

    /** Ready-to-encode QR content: {@code SKR1|<serial>|<productName>|<price>}. */
    private String qrPayload;
}
