package com.skr.erp.entity;

import com.skr.erp.common.constants.LedgerEntryType;
import com.skr.erp.common.constants.PaymentMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One money movement in an employee's payroll account (advance given or settlement paid).
 */
@Entity
@Table(name = "employee_ledger")
@Getter
@Setter
public class EmployeeLedger extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    // Month (slip) this transaction settles; null for a plain advance.
    @Column(name = "period")
    private LocalDate period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerEntryType type;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMode mode;

    @Column(length = 200)
    private String note;
}
