package com.skr.erp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateProductionRequest {

    @NotNull
    private UUID employeeId;
    @NotNull
    private LocalDate productionDate;
    @Valid
    @NotEmpty
    private List<ProductionItemRequest> items;
}