package com.skr.erp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVendorRequest {

    @NotBlank(message = "Vendor name is required")
    private String name;
    private String contactName;
    @NotBlank(message = "Mobile is required")
    private String mobile;
    @Email(message = "Invalid email")
    private String email;
    private String gstNumber;
    private String address;
}