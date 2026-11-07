package com.skr.erp.dto.request;

import com.skr.erp.common.constants.LedgerEntryType;
import com.skr.erp.common.constants.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecordLedgerRequest {

    @NotNull
    private LocalDate entryDate;
    @NotNull
    private LedgerEntryType type;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    private PaymentMode mode;
    private String note;
    // Set when settling a specific month's slip (first day of that month); null for a plain advance.
    private LocalDate period;
}
