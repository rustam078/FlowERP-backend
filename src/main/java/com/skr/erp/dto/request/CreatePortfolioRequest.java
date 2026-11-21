package com.skr.erp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** SUPER_ADMIN: a new portfolio, its locked 2-letter batch prefix, and its first admin user. */
@Data
public class CreatePortfolioRequest {

    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Batch prefix must be exactly 2 letters")
    private String batchPrefix;

    @NotBlank
    private String adminFullName;

    @NotBlank
    @Email(message = "Enter a valid admin email")
    private String adminEmail;

    @NotBlank
    private String adminPassword;
}
