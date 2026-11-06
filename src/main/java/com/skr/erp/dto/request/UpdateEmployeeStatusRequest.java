package com.skr.erp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateEmployeeStatusRequest {
    @NotNull
    private Boolean active;
}