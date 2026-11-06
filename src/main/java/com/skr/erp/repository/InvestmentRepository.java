package com.skr.erp.repository;

import com.skr.erp.entity.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvestmentRepository extends JpaRepository<Investment, UUID>, JpaSpecificationExecutor<Investment> {

    Optional<Investment> findTopByOrderByCreatedAtDesc();

    // Vendor's invoices oldest first — used to clear old dues before the new invoice.
    List<Investment> findByVendorIdOrderByPurchaseDateAscCreatedAtAsc(UUID vendorId);


    @Query("""
            SELECT i
            FROM Investment i
            WHERE
                (:vendorId IS NULL OR i.vendor.id = :vendorId)
            AND
                (CAST(:fromDate AS date) IS NULL OR i.purchaseDate >= CAST(:fromDate AS date))
            AND
                (CAST(:toDate AS date) IS NULL OR i.purchaseDate <= CAST(:toDate AS date))
            ORDER BY i.purchaseDate DESC
            """)
    List<Investment> search(@Param("vendorId") UUID vendorId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);


    @Query("""
            SELECT DISTINCT i
            FROM Investment i
            LEFT JOIN FETCH i.items
            WHERE i.vendor.id = :vendorId
            ORDER BY i.purchaseDate DESC, i.createdAt DESC
            """)
    List<Investment> findAllWithItemsByVendorId(UUID vendorId);

    @Query("""
            SELECT COUNT(i), COALESCE(SUM(i.grandTotal), 0)
            FROM Investment i
            WHERE i.purchaseDate >= :from AND i.purchaseDate <= :to
            """)
    List<Object[]> aggregateInvestmentBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT i.purchaseDate, COALESCE(SUM(i.grandTotal), 0)
            FROM Investment i
            WHERE i.purchaseDate >= :from AND i.purchaseDate <= :to
            GROUP BY i.purchaseDate
            ORDER BY i.purchaseDate
            """)
    List<Object[]> investmentSeriesBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}