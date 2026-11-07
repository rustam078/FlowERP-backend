package com.skr.erp.repository;

import com.skr.erp.common.constants.LedgerEntryType;
import com.skr.erp.entity.EmployeeLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface EmployeeLedgerRepository extends JpaRepository<EmployeeLedger, UUID> {

    List<EmployeeLedger> findByEmployeeIdOrderByEntryDateDescCreatedAtDesc(UUID employeeId);

    // Total money paid out to an employee under one entry type (advance or payout).
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM EmployeeLedger l WHERE l.employee.id = :employeeId AND l.type = :type")
    BigDecimal sumByEmployeeAndType(@Param("employeeId") UUID employeeId, @Param("type") LedgerEntryType type);

    // Global sum across all employees for the given entry types (used by the overall payroll KPI).
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM EmployeeLedger l WHERE l.type IN :types")
    BigDecimal sumByTypes(@Param("types") java.util.List<LedgerEntryType> types);
}
