package com.skr.erp.dto.response;

import com.skr.erp.common.constants.InvestmentItemType;
import com.skr.erp.common.constants.UnitType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class InvestmentItemResponse {

    private UUID id;
    private InvestmentItemType itemType;
    private UUID productId;
    private String itemName;
    private BigDecimal quantity;
    private UnitType unit;
    private BigDecimal rate;
    private BigDecimal sellingPrice;
    private BigDecimal totalAmount;
}