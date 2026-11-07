package com.skr.erp.dto.request;

import com.skr.erp.common.constants.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Give an advance to several employees at once (shared date/mode, per-employee amount).
 */
@Data
public class BulkAdvanceRequest {

    @NotNull
    private LocalDate entryDate;
    private PaymentMode mode;
    private String note;
    @NotEmpty
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull
        private UUID employeeId;
        @NotNull
        @DecimalMin("0.01")
        private BigDecimal amount;
    }
}
