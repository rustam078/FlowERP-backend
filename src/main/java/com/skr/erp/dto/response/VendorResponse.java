package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class VendorResponse {

    private UUID id;
    private String name;
    private String contactName;
    private String mobile;
    private String email;
    private String gstNumber;
    private String address;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}