package com.skr.erp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// Produce finished stock for one or more products in a single call (bulk, one-click).
@Data
public class ProduceRequest {
    private LocalDate date; // defaults to today when blank

    @NotEmpty(message = "Add at least one product to produce")
    @Valid
    private List<ProduceItemRequest> items;
}
