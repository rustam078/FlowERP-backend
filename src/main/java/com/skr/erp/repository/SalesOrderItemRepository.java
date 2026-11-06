package com.skr.erp.repository;

import com.skr.erp.entity.SalesOrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, UUID> {

    @Query("""
            SELECT i FROM SalesOrderItem i
            JOIN FETCH i.product
            WHERE i.salesOrder.id = :salesOrderId
            """)
    List<SalesOrderItem> findBySalesOrderIdWithProduct(
            @Param("salesOrderId") UUID salesOrderId);

    @Query("""
            SELECT COALESCE(SUM(i.lineTotal - (i.unitPrice * i.quantity)), 0)
            FROM SalesOrderItem i
            """)
    BigDecimal calculateTotalProfit();

    // Gross profit = revenue - cost of goods sold, for sales in the range.
    @Query("""
            SELECT COALESCE(SUM(i.lineTotal - (i.unitPrice * i.quantity)), 0)
            FROM SalesOrderItem i
            WHERE i.salesOrder.createdAt >= :start AND i.salesOrder.createdAt < :end
            """)
    BigDecimal grossProfitBetween(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end);

    // Cost of goods sold per day (unit cost * qty), for the daily report.
    @Query("""
            SELECT CAST(i.salesOrder.createdAt AS date), COALESCE(SUM(i.unitPrice * i.quantity), 0)
            FROM SalesOrderItem i
            WHERE i.salesOrder.createdAt >= :start AND i.salesOrder.createdAt < :end
            GROUP BY CAST(i.salesOrder.createdAt AS date)
            ORDER BY CAST(i.salesOrder.createdAt AS date)
            """)
    List<Object[]> cogsSeriesBetween(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end);

    @Query("""
            SELECT i.product.id, i.product.name,
                   SUM(i.quantity), COALESCE(SUM(i.lineTotal), 0)
            FROM SalesOrderItem i
            GROUP BY i.product.id, i.product.name
            ORDER BY SUM(i.quantity) DESC
            """)
    List<Object[]> findTopSellingProducts(Pageable pageable);

    @Query(
            value = """
                    SELECT i FROM SalesOrderItem i
                    JOIN i.salesOrder s
                    WHERE i.product.id = :productId
                    ORDER BY s.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(i) FROM SalesOrderItem i
                    WHERE i.product.id = :productId
                    """
    )
    Page<SalesOrderItem> findByProductId(
            @Param("productId") UUID productId,
            Pageable pageable);

    @Query("""
            SELECT i.product.name, SUM(i.quantity), COALESCE(SUM(i.lineTotal), 0)
            FROM SalesOrderItem i
            WHERE i.salesOrder.createdAt >= :start AND i.salesOrder.createdAt < :end
            GROUP BY i.product.id, i.product.name
            ORDER BY SUM(i.quantity) DESC
            """)
    List<Object[]> topProductsBetween(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end,
            Pageable pageable);

    // Per-product sales for a date range (report): product, qty, revenue, profit.
    @Query("""
            SELECT i.product.name, SUM(i.quantity), COALESCE(SUM(i.lineTotal), 0),
                   COALESCE(SUM(i.lineTotal - (i.unitPrice * i.quantity)), 0)
            FROM SalesOrderItem i
            WHERE i.salesOrder.createdAt >= :start AND i.salesOrder.createdAt < :end
            GROUP BY i.product.id, i.product.name
            ORDER BY SUM(i.lineTotal) DESC
            """)
    List<Object[]> salesByProductBetween(
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end,
            Pageable pageable);
}
