package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
public class VendorDetailsResponse {

    private UUID id;
    private String name;
    private String mobile;
    private String email;
    private String address;
    private Boolean active;
    private Integer totalInvoices;
    private BigDecimal totalPurchaseAmount;
    private LocalDate lastPurchaseDate;
    private List<VendorPurchaseResponse> purchases;
}