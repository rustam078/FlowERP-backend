package com.skr.erp.dto.response;
import com.skr.erp.common.constants.InvestmentItemType;
import com.skr.erp.common.constants.UnitType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Data
public class VendorPurchaseItemResponse {

    private UUID productId;
    private String itemName;
    private InvestmentItemType itemType;
    private BigDecimal quantity;
    private UnitType unit;
    private BigDecimal rate;
    private BigDecimal totalAmount;
}