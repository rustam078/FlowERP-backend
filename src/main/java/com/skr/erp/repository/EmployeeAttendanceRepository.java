package com.skr.erp.repository;

import com.skr.erp.entity.EmployeeAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeAttendanceRepository extends JpaRepository<EmployeeAttendance, UUID> {
    List<EmployeeAttendance> findByEmployeeIdAndDateBetween(UUID employeeId, LocalDate from, LocalDate to);
    Optional<EmployeeAttendance> findByEmployeeIdAndDate(UUID employeeId, LocalDate date);
    long countByEmployeeIdAndDateBetween(UUID employeeId, LocalDate from, LocalDate to);
}
