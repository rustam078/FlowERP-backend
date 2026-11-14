package com.skr.erp.repository;

import com.skr.erp.entity.EmployeeWorkPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeWorkPeriodRepository extends JpaRepository<EmployeeWorkPeriod, UUID> {

    // All inactive spells of an employee, earliest first — used by the salary engine.
    List<EmployeeWorkPeriod> findByEmployeeIdOrderByStartDateAsc(UUID employeeId);

    // Spells of several employees in one read (employee list).
    List<EmployeeWorkPeriod> findByEmployeeIdIn(Collection<UUID> employeeIds);

    // The still-open inactive spell (employee currently inactive), if any.
    Optional<EmployeeWorkPeriod> findByEmployeeIdAndEndDateIsNull(UUID employeeId);

    // The latest closed spell — its end date is the last reactivation date.
    Optional<EmployeeWorkPeriod> findTopByEmployeeIdAndEndDateIsNotNullOrderByEndDateDesc(UUID employeeId);
    @Query(value = """
    SELECT EXISTS (
        SELECT 1
        FROM employee_work_period ewp
        WHERE ewp.employee_id = :employeeId
          AND ewp.start_date <= :leaveEndDate
          AND (
              ewp.end_date IS NULL
              OR ewp.end_date > :leaveStartDate
          )
    )
    """, nativeQuery = true)
    boolean existsInactivePeriodOverlap(
            @Param("employeeId") UUID employeeId,
            @Param("leaveStartDate") LocalDate leaveStartDate,
            @Param("leaveEndDate") LocalDate leaveEndDate
    );

    @Query("""
    SELECT wp
    FROM EmployeeWorkPeriod wp
    WHERE wp.employee.id = :employeeId
      AND wp.startDate <= :to
      AND (wp.endDate IS NULL OR wp.endDate > :from)
    """)
    List<EmployeeWorkPeriod> findOverlappingPeriods(@Param("employeeId") UUID employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
