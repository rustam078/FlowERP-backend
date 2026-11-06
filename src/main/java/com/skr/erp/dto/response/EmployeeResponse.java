package com.skr.erp.dto.response;

import com.skr.erp.common.constants.PaymentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
@Data
@Builder
public class EmployeeResponse {
    private UUID id;
    private String employeeCode;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String address;
    private LocalDate joiningDate;
    private Boolean active;
    private PaymentType paymentType;
    private BigDecimal monthlySalary;
    private BigDecimal openingAdvance;
    private BigDecimal currentMonthEarning;
    private BigDecimal totalEarning;

}
