package com.skr.erp.repository;

import com.skr.erp.entity.ProductionEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ProductionEntryRepository extends JpaRepository<ProductionEntry, UUID>, JpaSpecificationExecutor<ProductionEntry> {

    // One employee's entries (with details) in a single query, optionally date-bounded.
    @Query("SELECT DISTINCT pe FROM ProductionEntry pe LEFT JOIN FETCH pe.details WHERE pe.employee.id = :employeeId AND (CAST(:from AS date) IS NULL OR pe.productionDate >= :from) AND (CAST(:to AS date) IS NULL OR pe.productionDate <= :to)")
    List<ProductionEntry> findByEmployeeWithDetails(@Param("employeeId") UUID employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}