package com.skr.erp.repository;

import com.skr.erp.common.constants.LedgerEntryType;
import com.skr.erp.entity.EmployeeLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeLedgerRepository extends JpaRepository<EmployeeLedger, UUID>, JpaSpecificationExecutor<EmployeeLedger> {

    List<EmployeeLedger> findByEmployeeIdOrderByEntryDateDescCreatedAtDesc(UUID employeeId);

    // Newest entry of one type (only the newest advance / expense may be deleted).
    Optional<EmployeeLedger> findFirstByEmployeeIdAndTypeOrderByEntryDateDescCreatedAtDesc(UUID employeeId, LedgerEntryType type);

    // Per-type totals for one employee in a single grouped read.
    @Query("SELECT l.type, COALESCE(SUM(l.amount), 0) FROM EmployeeLedger l WHERE l.employee.id = :employeeId GROUP BY l.type")
    List<Object[]> sumByTypeForEmployee(@Param("employeeId") UUID employeeId);

    // Settled amount (direct payout + advance adjustment) per slip month within a range.
    @Query("SELECT l.period, COALESCE(SUM(l.amount), 0) FROM EmployeeLedger l WHERE l.employee.id = :employeeId "
            + "AND l.type IN (com.skr.erp.common.constants.LedgerEntryType.PAYOUT, "
            + "com.skr.erp.common.constants.LedgerEntryType.ADVANCE_ADJUSTMENT) "
            + "AND l.period BETWEEN :from AND :to GROUP BY l.period")
    List<Object[]> sumSettledByPeriod(@Param("employeeId") UUID employeeId,
                                      @Param("from") LocalDate from, @Param("to") LocalDate to);

    // Latest settlement date of one slip month (null when it has not been settled).
    @Query("SELECT MAX(l.entryDate) FROM EmployeeLedger l WHERE l.employee.id = :employeeId AND l.period = :period "
            + "AND l.type IN (com.skr.erp.common.constants.LedgerEntryType.PAYOUT, "
            + "com.skr.erp.common.constants.LedgerEntryType.ADVANCE_ADJUSTMENT)")
    LocalDate lastSettlementDate(@Param("employeeId") UUID employeeId, @Param("period") LocalDate period);

    // Total money paid out to an employee under one entry type (advance or payout).
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM EmployeeLedger l WHERE l.employee.id = :employeeId AND l.type = :type")
    BigDecimal sumByEmployeeAndType(@Param("employeeId") UUID employeeId, @Param("type") LedgerEntryType type);

    // Global sum across all employees for the given entry types (used by the overall payroll KPI).
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM EmployeeLedger l WHERE l.type IN :types")
    BigDecimal sumByTypes(@Param("types") java.util.List<LedgerEntryType> types);

    // Money given (advance + expense + direct payment) grouped by employee — for the scalable list.
    @Query("SELECT l.employee.id, SUM(CASE WHEN l.type IN (com.skr.erp.common.constants.LedgerEntryType.ADVANCE, "
            + "com.skr.erp.common.constants.LedgerEntryType.SPEND, com.skr.erp.common.constants.LedgerEntryType.PAYOUT) "
            + "THEN l.amount ELSE 0 END) FROM EmployeeLedger l GROUP BY l.employee.id")
    java.util.List<Object[]> sumMoneyGivenGroupedByEmployee();
}
