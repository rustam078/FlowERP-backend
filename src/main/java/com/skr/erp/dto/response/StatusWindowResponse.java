package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Allowed date range for an employee's next status change (forward-only, never in the future).
 */
@Data
@Builder
public class StatusWindowResponse {
    private String action;      // INACTIVATE | REACTIVATE
    private LocalDate minDate;  // earliest allowed date (null = no lower bound)
    private LocalDate maxDate;  // latest allowed date (today)
}
