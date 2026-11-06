package com.skr.erp.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ProductionItemRequest {

    private UUID productId;
    private UUID pieceCodeId;
    private Integer quantity;
}