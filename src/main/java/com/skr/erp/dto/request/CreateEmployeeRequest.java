package com.skr.erp.dto.request;

import com.skr.erp.common.constants.PaymentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
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

    // Pay model (defaults to PIECE_RATE); monthlySalary used only for MONTHLY; openingAdvance optional.
    private PaymentType paymentType;
    private BigDecimal monthlySalary;
    private BigDecimal openingAdvance;
}