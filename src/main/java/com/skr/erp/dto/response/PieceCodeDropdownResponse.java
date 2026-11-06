package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
public class PieceCodeDropdownResponse {
    private UUID id;
    private String code;
    private BigDecimal rate;
}