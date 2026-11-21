package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class UserResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String role;
    private Boolean active;
}
