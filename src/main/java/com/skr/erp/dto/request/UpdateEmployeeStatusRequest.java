package com.skr.erp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateEmployeeStatusRequest {
    @NotNull
    private Boolean active;

    // Deactivating → the last working date (salary stops after it).
    // Activating → the rejoin date (salary resumes from it). Defaults to today when omitted.
    private LocalDate effectiveDate;
}