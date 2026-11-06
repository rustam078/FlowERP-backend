package com.skr.erp.dto.response;

import com.skr.erp.dto.response.EmployeeResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmployeeDetailsResponse {

    private EmployeeResponse employee;
    private EmployeeSummaryResponse summary;
    private List<EmployeeProductSummaryResponse> productSummary;
    private List<EmployeeProductionHistoryResponse> productionHistory;
    private List<EmployeeCurrentMonthProductResponse> currentMonthProductSummary;

}