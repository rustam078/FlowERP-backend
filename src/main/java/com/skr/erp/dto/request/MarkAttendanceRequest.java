package com.skr.erp.dto.request;

import com.skr.erp.common.constants.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

// Mark one day as an exception (absent / half-day) for an employee.
@Data
public class MarkAttendanceRequest {
    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;
}
