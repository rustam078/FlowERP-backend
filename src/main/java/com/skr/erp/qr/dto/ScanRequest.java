package com.skr.erp.qr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScanRequest {

    /** Raw scanned content: either the full QR payload or a bare serial. */
    @NotBlank
    private String code;
}
