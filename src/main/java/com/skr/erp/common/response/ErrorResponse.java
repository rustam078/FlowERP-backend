package com.skr.erp.common.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
    // Short id of the request (also in the logs and the audit log) — shown to the user as "Ref".
    private String requestId;
}