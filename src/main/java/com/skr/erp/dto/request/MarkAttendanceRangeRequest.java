package com.skr.erp.dto.request;

import com.skr.erp.common.constants.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

// Mark a date range (inclusive) as an exception (absent / half-day) in one go.
@Data
public class MarkAttendanceRangeRequest {
    @NotNull(message = "From date is required")
    private LocalDate fromDate;

    @NotNull(message = "To date is required")
    private LocalDate toDate;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;
}
