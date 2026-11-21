package com.skr.erp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Change the signed-in user's own password (the user comes from the session, not the body). */
@Data
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    private String newPassword;
}
