package com.skr.erp.repository;

import com.skr.erp.entity.EmployeeWorkPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeWorkPeriodRepository extends JpaRepository<EmployeeWorkPeriod, UUID> {

    // All spells for an employee, earliest first — used by the salary engine.
    List<EmployeeWorkPeriod> findByEmployeeIdOrderByStartDateAsc(UUID employeeId);

    // The currently-open spell (not yet ended), if the employee is working.
    Optional<EmployeeWorkPeriod> findByEmployeeIdAndEndDateIsNull(UUID employeeId);
    Optional<EmployeeWorkPeriod> findTopByEmployeeIdAndEndDateIsNotNullOrderByEndDateDesc(UUID employeeId);
}
