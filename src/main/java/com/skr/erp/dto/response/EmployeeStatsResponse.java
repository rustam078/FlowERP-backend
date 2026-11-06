package com.skr.erp.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EmployeeStatsResponse {
private Long totalEmployees;
private Long activeEmployees;
private Long inactiveEmployees;
private BigDecimal currentMonthTotalEarning;
private BigDecimal overallTotalEarning;

}
