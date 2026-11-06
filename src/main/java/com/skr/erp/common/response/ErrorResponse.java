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
}