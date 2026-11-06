package com.skr.erp.dto.response;

import com.skr.erp.common.constants.RateStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ProductRateResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private BigDecimal rate;
    private LocalDate effectiveFrom;
    private RateStatus status;
}