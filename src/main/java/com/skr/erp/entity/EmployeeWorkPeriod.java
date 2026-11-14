package com.skr.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// One INACTIVE spell: startDate = inactive date (first non-working day), endDate = reactivation date
// (a working day again). endDate stays null while the employee is still inactive.
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

    // Null while the employee is still inactive.
    @Column(name = "end_date")
    private LocalDate endDate;
}
