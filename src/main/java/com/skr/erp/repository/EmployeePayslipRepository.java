package com.skr.erp.repository;

import com.skr.erp.entity.EmployeePayslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeePayslipRepository extends JpaRepository<EmployeePayslip, UUID> {

    // All-time totals of the saved months: earned salary and unsettled due.
    interface Totals {
        BigDecimal getEarned();
        BigDecimal getDue();
    }

    // Saved months of an employee (light read used to find the missing months).
    @Query("SELECT p.period FROM EmployeePayslip p WHERE p.employee.id = :employeeId")
    List<LocalDate> findPeriodsByEmployeeId(@Param("employeeId") UUID employeeId);

    Optional<EmployeePayslip> findByEmployeeIdAndPeriod(UUID employeeId, LocalDate period);

    // One calendar year of saved months, newest first.
    List<EmployeePayslip> findByEmployeeIdAndPeriodBetweenOrderByPeriodDesc(UUID employeeId, LocalDate from, LocalDate to);

    // Saved months that may still change (not yet fully paid), oldest first.
    List<EmployeePayslip> findByEmployeeIdAndFinalizedFalseAndPeriodBetweenOrderByPeriodAsc(
            UUID employeeId, LocalDate from, LocalDate to);

    // Unsettled months with money due, oldest first (advance auto-settle order).
    List<EmployeePayslip> findByEmployeeIdAndFinalizedFalseAndDueGreaterThanOrderByPeriodAsc(UUID employeeId, BigDecimal due);

    // The newest locked (fully paid) month — nothing on or before it may change.
    Optional<EmployeePayslip> findTopByEmployeeIdAndFinalizedTrueOrderByPeriodDesc(UUID employeeId);

    boolean existsByEmployeeIdAndFinalizedTrueAndPeriodBetween(UUID employeeId, LocalDate from, LocalDate to);

    boolean existsByEmployeeIdAndFinalizedTrue(UUID employeeId);

    @Query("SELECT COALESCE(SUM(p.earned), 0) AS earned, COALESCE(SUM(p.due), 0) AS due "
            + "FROM EmployeePayslip p WHERE p.employee.id = :employeeId")
    Totals totalsByEmployee(@Param("employeeId") UUID employeeId);

    // Saved-month count and earned total per employee — one read for the employee list.
    @Query("SELECT p.employee.id, COUNT(p), COALESCE(SUM(p.earned), 0) FROM EmployeePayslip p "
            + "WHERE p.employee.id IN :employeeIds GROUP BY p.employee.id")
    List<Object[]> countAndEarnedByEmployees(@Param("employeeIds") Collection<UUID> employeeIds);

    @Modifying
    @Query("DELETE FROM EmployeePayslip p WHERE p.employee.id = :employeeId")
    void deleteAllByEmployeeId(@Param("employeeId") UUID employeeId);
}
