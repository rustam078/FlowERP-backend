package com.skr.erp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateEmployeeRequest {

    @NotBlank
    private String fullName;
    private String mobileNumber;
    private String address;
    private String email;
    @NotNull
    private LocalDate joiningDate;
    @NotNull
    private Boolean active;
}