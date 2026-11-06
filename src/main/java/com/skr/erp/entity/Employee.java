package com.skr.erp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

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
}