package com.skr.erp.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ProductionFilterRequest {

    private LocalDate fromDate;
    private LocalDate toDate;
    private UUID employeeId;
}