package com.skr.erp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateEmployeeRequest {

    @NotBlank
    private String fullName;
    private String mobileNumber;
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    private String address;
    @NotNull
    private LocalDate joiningDate;
}