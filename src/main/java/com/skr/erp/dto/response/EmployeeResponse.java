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
    private BigDecimal totalEarning;     // total money given = advance + expense + payment
    private BigDecimal advanceBalance;   // current unsettled advance in the employee's wallet
    private BigDecimal pending;          // still owed to the employee (0 when nothing pending)

}
