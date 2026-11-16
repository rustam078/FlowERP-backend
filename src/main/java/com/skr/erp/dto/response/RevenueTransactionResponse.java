package com.skr.erp.dto.response;

import com.skr.erp.common.constants.RevenueModule;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** One money movement shown in Revenue, read live from its own module's payment record. */
@Data
@Builder
public class RevenueTransactionResponse {
    private UUID id;
    private RevenueModule module;
    private String type;          // SALE_PAYMENT / DUE_CLEARED / DUE_SETTLEMENT, PURCHASE / OVERHEAD, ADVANCE / PAYOUT / SPEND
    private LocalDate date;
    private String reference;     // invoice no (sales / investment) or employee code
    private String note;          // clearing invoice no, payment reference or ledger note
    private String party;         // customer / vendor / employee name
    private String contact;       // their mobile number
    private String paymentMode;
    private BigDecimal amount;
    private String direction;     // IN (received) or OUT (paid)
    private UUID sourceId;        // sale / investment / employee id
}
