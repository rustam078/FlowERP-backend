package com.skr.erp.repository;

import com.skr.erp.entity.EmployeeAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeAttendanceRepository extends JpaRepository<EmployeeAttendance, UUID> {
    List<EmployeeAttendance> findByEmployeeIdAndDateBetween(UUID employeeId, LocalDate from, LocalDate to);
    // Leave of several employees in one read (employee list: current-month salary).
    List<EmployeeAttendance> findByEmployeeIdInAndDateBetween(Collection<UUID> employeeIds, LocalDate from, LocalDate to);
    Optional<EmployeeAttendance> findByEmployeeIdAndDate(UUID employeeId, LocalDate date);
    long countByEmployeeIdAndDateBetween(UUID employeeId, LocalDate from, LocalDate to);
    @Modifying
    @Query("""
    DELETE FROM EmployeeAttendance ea
    WHERE ea.employee.id = :employeeId
      AND ea.date >= :startDate
      AND ea.date < :endDate
    """)
    int deleteLeavesByEmployeeAndDateRange(
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
