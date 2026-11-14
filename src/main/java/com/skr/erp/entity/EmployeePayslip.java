package com.skr.erp.entity;

import com.skr.erp.common.constants.PayslipMonthStatus;
import com.skr.erp.common.constants.PayslipPaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Saved salary slip for one completed month (monthly staff). The current month is never stored.
// Unfinalized rows are re-snapshotted as payments / attendance change; a fully paid month is locked.
@Entity
@Table(name = "employee_payslip",
        uniqueConstraints = @UniqueConstraint(name = "uq_payslip_emp_period", columnNames = {"employee_id", "period"}))
@Getter
@Setter
public class EmployeePayslip extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // First day of the month this slip covers.
    @Column(nullable = false)
    private LocalDate period;

    @Enumerated(EnumType.STRING)
    @Column(name = "month_status", nullable = false, length = 10)
    private PayslipMonthStatus monthStatus;

    @Column(name = "active_days", nullable = false)
    private int activeDays;

    @Column(name = "month_days", nullable = false)
    private int monthDays;

    // Salary in force when the month was saved — later salary edits never rewrite history.
    @Column(name = "monthly_salary", nullable = false, precision = 18, scale = 2)
    private BigDecimal monthlySalary;

    @Column(name = "base_salary", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "leave_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal leaveDays;

    @Column(name = "leave_deduction", nullable = false, precision = 18, scale = 2)
    private BigDecimal leaveDeduction;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal earned;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal paid;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal due;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 10)
    private PayslipPaymentStatus paymentStatus;

    // True once fully paid; a finalized month never changes again.
    @Column(nullable = false)
    private boolean finalized;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;
}
