package com.skr.erp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String role; // ADMIN | STAFF

    private Boolean active;
}
