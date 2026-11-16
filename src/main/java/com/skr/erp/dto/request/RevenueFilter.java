package com.skr.erp.dto.request;

import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.RevenueModule;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Optional filters for the Revenue transaction list (bound from query params; all empty = everything). */
@Data
public class RevenueFilter {

    private RevenueModule module;

    private String type;

    private PaymentMode paymentMode;

    private String search;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;
}
