package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class EmployeeCalendarResponse {
private LocalDate date;
private Integer totalQuantity;
private BigDecimal totalEarning;


}
