package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** A customer's unpaid balance across all their sales, for the sale form. */
@Data
@Builder
public class CustomerOutstandingResponse {

    private BigDecimal totalOutstanding;
    private int unpaidCount;
}
