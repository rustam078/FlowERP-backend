package com.skr.erp.repository;

import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import com.skr.erp.dto.response.SalesSummaryProjection;
import com.skr.erp.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID>, JpaSpecificationExecutor<SalesOrder> {

    boolean existsByInvoiceNo(String invoiceNumber);

    Page<SalesOrder> findByCustomerMobileOrderByCreatedAtDesc(String customerMobile, Pageable pageable);

    // A customer's unpaid sales, oldest first — the ledger a new payment clears in order.
    @Query("""
                SELECT s FROM SalesOrder s
                WHERE s.customerMobile = :mobile AND s.amountPaid < s.grandTotal
                ORDER BY s.createdAt ASC
            """)
    List<SalesOrder> findOutstandingByMobile(@Param("mobile") String mobile);

    @Query("""
                SELECT
                    COUNT(s) AS totalSales,
                    COALESCE(SUM(s.grandTotal), 0) AS totalRevenue
                FROM SalesOrder s
                WHERE s.createdAt >= :start
                  AND s.createdAt < :end
            """)
    SalesSummaryProjection countAndRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
                SELECT
                    COUNT(s) AS totalSales,
                    COALESCE(SUM(s.grandTotal), 0) AS totalRevenue
                FROM SalesOrder s
            """)
    SalesSummaryProjection countAndTotalRevenue();

    // Sales totals for a date range (report summary): count, subtotal, discount, tax, grand total,
    // amount received (actual cash tendered), due (summed per-invoice, floored at 0 so never negative).
    @Query("""
                SELECT COUNT(s), COALESCE(SUM(s.subtotal), 0), COALESCE(SUM(s.discount), 0),
                       COALESCE(SUM(s.tax), 0), COALESCE(SUM(s.grandTotal), 0),
                       COALESCE(SUM(s.amountReceived), 0),
                       COALESCE(SUM(CASE WHEN s.grandTotal > s.amountPaid THEN s.grandTotal - s.amountPaid ELSE 0 END), 0)
                FROM SalesOrder s
                WHERE s.createdAt >= :start AND s.createdAt < :end
            """)
    List<Object[]> reportSummaryBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Invoice-wise detail for the sales report, latest first (matches the on-screen table order).
    // Received = amountReceived (actual cash tendered); amountPaid drives the per-invoice due + status.
    // Pageable lets the PDF fetch only the top N rows while Excel pulls them all (Pageable.unpaged()).
    @Query("""
                SELECT s.invoiceNo, s.createdAt, s.customerName, s.customerMobile,
                       s.grandTotal, s.amountReceived, s.amountPaid
                FROM SalesOrder s
                WHERE s.createdAt >= :start AND s.createdAt < :end
                ORDER BY s.createdAt DESC
            """)
    List<Object[]> salesInvoicesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    // Filter-aware summary totals computed in SQL (count, revenue, discount, received, due) — avoids
    // loading every matching sale into memory for the list header. received = actual cash tendered;
    // due is summed per-invoice and floored at 0 so it can never go negative.
    @Query("""
                SELECT COUNT(s), COALESCE(SUM(s.grandTotal), 0), COALESCE(SUM(s.discount), 0),
                       COALESCE(SUM(s.amountReceived), 0),
                       COALESCE(SUM(CASE WHEN s.grandTotal > s.amountPaid THEN s.grandTotal - s.amountPaid ELSE 0 END), 0)
                FROM SalesOrder s
                WHERE (:search IS NULL OR LOWER(s.invoiceNo) LIKE :search OR LOWER(s.customerName) LIKE :search OR LOWER(s.customerMobile) LIKE :search)
                  AND (CAST(:start AS timestamp) IS NULL OR s.createdAt >= :start)
                  AND (CAST(:end AS timestamp) IS NULL OR s.createdAt < :end)
                  AND (:paymentMode IS NULL OR s.paymentMode = :paymentMode)
                  AND (:paymentStatus IS NULL OR s.paymentStatus = :paymentStatus)
            """)
    List<Object[]> summaryFiltered(@Param("search") String search, @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end, @Param("paymentMode") PaymentMode paymentMode,
                                   @Param("paymentStatus") PaymentStatus paymentStatus);

    // ── Dashboard aggregations ───────────────────────────
    // count, revenue, discount, received (actual cash tendered), due (per-invoice floored at 0).
    @Query("""
                SELECT COUNT(s), COALESCE(SUM(s.grandTotal), 0), COALESCE(SUM(s.discount), 0),
                       COALESCE(SUM(s.amountReceived), 0),
                       COALESCE(SUM(CASE WHEN s.grandTotal > s.amountPaid THEN s.grandTotal - s.amountPaid ELSE 0 END), 0)
                FROM SalesOrder s
                WHERE s.createdAt >= :start AND s.createdAt < :end
            """)
    List<Object[]> aggregateSalesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
                SELECT CAST(s.createdAt AS date), COALESCE(SUM(s.grandTotal), 0), COUNT(s)
                FROM SalesOrder s
                WHERE s.createdAt >= :start AND s.createdAt < :end
                GROUP BY CAST(s.createdAt AS date)
                ORDER BY CAST(s.createdAt AS date)
            """)
    List<Object[]> revenueSeriesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
                SELECT s.paymentMode, COUNT(s), COALESCE(SUM(s.amountPaid), 0)
                FROM SalesOrder s
                WHERE s.createdAt >= :start AND s.createdAt < :end
                GROUP BY s.paymentMode
            """)
    List<Object[]> paymentBreakdownBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
                SELECT s.customerName, s.customerMobile, COUNT(s), COALESCE(SUM(s.grandTotal), 0)
                FROM SalesOrder s
                WHERE s.createdAt >= :start AND s.createdAt < :end
                GROUP BY s.customerMobile, s.customerName
                ORDER BY COUNT(s) DESC
            """)
    List<Object[]> topCustomersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    // Mobiles whose FIRST-ever order falls in the range = new customers acquired in the period.
    @Query("""
                SELECT s.customerMobile
                FROM SalesOrder s
                GROUP BY s.customerMobile
                HAVING MIN(s.createdAt) >= :start AND MIN(s.createdAt) < :end
            """)
    List<String> newCustomerMobilesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT s FROM SalesOrder s ORDER BY s.createdAt DESC")
    List<SalesOrder> findRecent(Pageable pageable);
}
