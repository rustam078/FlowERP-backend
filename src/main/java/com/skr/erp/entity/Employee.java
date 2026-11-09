package com.skr.erp.entity;

import com.skr.erp.common.constants.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
@Getter
@Setter
@BatchSize(size = 100)
public class Employee extends BaseEntity {

    @Column(name = "employee_code", nullable = false, unique = true)
    private String employeeCode;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(unique = true)
    private String email;

    private String address;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(nullable = false)
    private Boolean active = true;

    // Pay model: PIECE_RATE (earns per piece-work) or MONTHLY (fixed basic salary).
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 20)
    private PaymentType paymentType = PaymentType.PIECE_RATE;

    // Basic salary for MONTHLY employees (null for piece-rate).
    @Column(name = "monthly_salary", precision = 12, scale = 2)
    private BigDecimal monthlySalary;

    // Running advance balance, maintained on each ledger write (O(1) read — no full-ledger scan).
    // Opening advance is recorded as the first ADVANCE ledger entry — no separate column.
    @Column(name = "advance_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal advanceBalance = BigDecimal.ZERO;
}