package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PieceCodeResponse {

    private UUID id;
    private UUID productId;
    private String productName;
    private String code;
    private BigDecimal rate;
    private Boolean active;
    private Boolean used;
    private LocalDateTime createdAt;
}