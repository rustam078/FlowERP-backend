package com.skr.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// One continuous spell of employment. An open period (endDate null) means currently employed.
// Deactivating closes the open period at the last working date; reactivating opens a new one.
@Entity
@Table(name = "employee_work_period")
@Getter
@Setter
public class EmployeeWorkPeriod extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // Null while the employee is still working in this spell.
    @Column(name = "end_date")
    private LocalDate endDate;
}
