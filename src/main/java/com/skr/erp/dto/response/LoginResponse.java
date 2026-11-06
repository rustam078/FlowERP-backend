package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LoginResponse {

    private UUID userId;
    private String username;
    private String fullName;
    private String role;
}