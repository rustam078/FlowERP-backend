package com.skr.erp.repository;

import com.skr.erp.entity.ProductionEntryDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProductionEntryDetailRepository extends JpaRepository<ProductionEntryDetail, UUID> {
    boolean existsByProductId(UUID productId);

    boolean existsByPieceCodeId(UUID pieceCodeId);

    @Query("""
            SELECT COUNT(DISTINCT d.productionEntry.id),
                   COALESCE(SUM(d.quantity), 0),
                   COALESCE(SUM(d.amountSnapshot), 0)
            FROM ProductionEntryDetail d
            WHERE d.productionEntry.productionDate >= :from
              AND d.productionEntry.productionDate <= :to
            """)
    List<Object[]> aggregateProductionBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT e.fullName,
                   COALESCE(SUM(d.quantity), 0),
                   COALESCE(SUM(d.amountSnapshot), 0)
            FROM ProductionEntryDetail d
            JOIN d.productionEntry pe
            JOIN pe.employee e
            WHERE pe.productionDate >= :from AND pe.productionDate <= :to
            GROUP BY e.id, e.fullName
            ORDER BY SUM(d.quantity) DESC
            """)
    List<Object[]> topEmployeesBetween(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Query("""
            SELECT pe.productionDate, COALESCE(SUM(d.amountSnapshot), 0)
            FROM ProductionEntryDetail d
            JOIN d.productionEntry pe
            WHERE pe.productionDate >= :from AND pe.productionDate <= :to
            GROUP BY pe.productionDate
            ORDER BY pe.productionDate
            """)
    List<Object[]> wagesSeriesBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Filtered aggregate [entries, qty, amount] for the production summary cards.
    @Query("""
            SELECT COUNT(DISTINCT pe.id),
                   COALESCE(SUM(d.quantity), 0),
                   COALESCE(SUM(d.amountSnapshot), 0)
            FROM ProductionEntryDetail d
            JOIN d.productionEntry pe
            WHERE (CAST(:from AS date) IS NULL OR pe.productionDate >= :from)
              AND (CAST(:to AS date) IS NULL OR pe.productionDate <= :to)
              AND (:employeeId IS NULL OR pe.employee.id = :employeeId)
              AND (:productId IS NULL OR d.product.id = :productId)
            """)
    List<Object[]> productionStats(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                   @Param("employeeId") UUID employeeId, @Param("productId") UUID productId);

    // Production grouped by product for a date range (report: which product, how much, payment).
    @Query("""
            SELECT d.productNameSnapshot,
                   COALESCE(SUM(d.quantity), 0),
                   COALESCE(SUM(d.amountSnapshot), 0)
            FROM ProductionEntryDetail d
            JOIN d.productionEntry pe
            WHERE pe.productionDate >= :from AND pe.productionDate <= :to
            GROUP BY d.productNameSnapshot
            ORDER BY SUM(d.quantity) DESC
            """)
    List<Object[]> productionByProductBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Earning (sum of amountSnapshot) per employee — one query for the whole employee list.
    @Query("SELECT pe.employee.id, COALESCE(SUM(d.amountSnapshot), 0) FROM ProductionEntryDetail d JOIN d.productionEntry pe GROUP BY pe.employee.id")
    List<Object[]> sumEarningGroupedByEmployee();

    @Query("SELECT pe.employee.id, COALESCE(SUM(d.amountSnapshot), 0) FROM ProductionEntryDetail d JOIN d.productionEntry pe WHERE pe.productionDate >= :from AND pe.productionDate <= :to GROUP BY pe.employee.id")
    List<Object[]> sumEarningGroupedByEmployeeBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(d.amountSnapshot), 0) FROM ProductionEntryDetail d WHERE d.productionEntry.employee.id = :employeeId")
    BigDecimal sumEarningByEmployee(@Param("employeeId") UUID employeeId);

    @Query("SELECT COALESCE(SUM(d.amountSnapshot), 0) FROM ProductionEntryDetail d WHERE d.productionEntry.employee.id = :employeeId AND d.productionEntry.productionDate >= :from AND d.productionEntry.productionDate <= :to")
    BigDecimal sumEarningByEmployeeBetween(@Param("employeeId") UUID employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(d.amountSnapshot), 0) FROM ProductionEntryDetail d")
    BigDecimal sumAllEarning();

    @Query("SELECT COALESCE(SUM(d.amountSnapshot), 0) FROM ProductionEntryDetail d WHERE d.productionEntry.productionDate >= :from AND d.productionEntry.productionDate <= :to")
    BigDecimal sumAllEarningBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}