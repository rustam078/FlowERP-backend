package com.skr.erp.dto.response;

import com.skr.erp.common.constants.LedgerEntryType;
import com.skr.erp.common.constants.PaymentMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class LedgerEntryResponse {
    private UUID id;
    private LocalDate entryDate;
    private LedgerEntryType type;
    private BigDecimal amount;
    private PaymentMode mode;
    private String note;
    private LocalDate period;
    private boolean deletable;   // only plain advances can be removed
}
