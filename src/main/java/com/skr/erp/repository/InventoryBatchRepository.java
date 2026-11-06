package com.skr.erp.repository;

import com.skr.erp.common.constants.InventoryBatchStatus;
import com.skr.erp.entity.InventoryBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, UUID> {

    List<InventoryBatch> findByProductId(UUID productId);

    java.util.Optional<InventoryBatch> findByBatchNumber(String batchNumber);

    @Query("""
            SELECT b FROM InventoryBatch b
            WHERE b.product.id = :productId
            AND b.status = com.skr.erp.common.constants.InventoryBatchStatus.ACTIVE
            AND b.source = 'MANUFACTURED'
            AND b.unitCost = :unitCost
            AND b.sellingPrice = :sellingPrice
            ORDER BY b.receivedDate DESC, b.createdAt DESC
            """)
    List<InventoryBatch> findMergeableBatches(@Param("productId") UUID productId,
                                              @Param("unitCost") BigDecimal unitCost,
                                              @Param("sellingPrice") BigDecimal sellingPrice);

    java.util.Optional<InventoryBatch> findFirstBySourceAndSourceId(String source, UUID sourceId);

    @Query(value = "SELECT nextval('inventory_batch_seq')", nativeQuery = true)
    Long getNextBatchSequence();

    // Highest numeric part of an existing BT###### batch number (0 when table is empty).
    @Query(value = "SELECT COALESCE(MAX(CAST(substring(batch_number from 3) AS integer)), 0) FROM inventory_batch WHERE batch_number ~ '^BT[0-9]+$'", nativeQuery = true)
    long getMaxBatchNumberValue();

    @Query(value = "SELECT setval('inventory_batch_seq', :value)", nativeQuery = true)
    long resetBatchSequence(@Param("value") long value);

    @Query(value = """
                SELECT b FROM InventoryBatch b
                JOIN FETCH b.product p
            """, countQuery = """
                SELECT COUNT(b) FROM InventoryBatch b
            """)
    Page<InventoryBatch> findAllWithProduct(Pageable pageable);

    @Query("""
            SELECT b FROM InventoryBatch b
            JOIN FETCH b.product p
            WHERE p.id = :productId
            ORDER BY b.createdAt DESC
            """)
    List<InventoryBatch> findAllByProductId(@Param("productId") UUID productId);

    @Query("""
            SELECT b FROM InventoryBatch b
            JOIN FETCH b.product p
            WHERE p.id = :productId
            AND b.receivedDate >= :fromDate
            AND b.receivedDate <= :toDate
            ORDER BY b.createdAt DESC
            """)
    List<InventoryBatch> findAllByProductIdAndReceivedDateBetween(@Param("productId") UUID productId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query(value = "SELECT b FROM InventoryBatch b JOIN FETCH b.product WHERE b.product.id = :productId ORDER BY b.receivedDate DESC, b.batchNumber DESC",
            countQuery = "SELECT COUNT(b) FROM InventoryBatch b WHERE b.product.id = :productId")
    Page<InventoryBatch> pageByProductId(@Param("productId") UUID productId, Pageable pageable);

    @Query(value = "SELECT b FROM InventoryBatch b JOIN FETCH b.product WHERE b.product.id = :productId AND b.receivedDate BETWEEN :fromDate AND :toDate ORDER BY b.receivedDate DESC, b.batchNumber DESC",
            countQuery = "SELECT COUNT(b) FROM InventoryBatch b WHERE b.product.id = :productId AND b.receivedDate BETWEEN :fromDate AND :toDate")
    Page<InventoryBatch> pageByProductIdAndReceivedDateBetween(@Param("productId") UUID productId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate, Pageable pageable);

    // Batch history filtered by status (Active / Sold tab).
    @Query(value = "SELECT b FROM InventoryBatch b JOIN FETCH b.product WHERE b.product.id = :productId AND b.status = :status ORDER BY b.receivedDate DESC, b.batchNumber DESC",
            countQuery = "SELECT COUNT(b) FROM InventoryBatch b WHERE b.product.id = :productId AND b.status = :status")
    Page<InventoryBatch> pageByProductIdAndStatus(@Param("productId") UUID productId, @Param("status") InventoryBatchStatus status, Pageable pageable);

    @Query(value = "SELECT b FROM InventoryBatch b JOIN FETCH b.product WHERE b.product.id = :productId AND b.receivedDate BETWEEN :fromDate AND :toDate AND b.status = :status ORDER BY b.receivedDate DESC, b.batchNumber DESC",
            countQuery = "SELECT COUNT(b) FROM InventoryBatch b WHERE b.product.id = :productId AND b.receivedDate BETWEEN :fromDate AND :toDate AND b.status = :status")
    Page<InventoryBatch> pageByProductIdAndReceivedDateBetweenAndStatus(@Param("productId") UUID productId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate, @Param("status") InventoryBatchStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.quantityReceived),0), COALESCE(SUM(b.quantityAvailable),0), COALESCE(SUM(b.quantityAvailable * b.unitCost),0) FROM InventoryBatch b WHERE b.product.id = :productId")
    List<Object[]> aggregateByProductId(@Param("productId") UUID productId);

    // Quantity-weighted selling price per product from in-stock batches: [productId, SUM(available*sellingPrice), SUM(available)].
    @Query("""
            SELECT b.product.id, COALESCE(SUM(b.quantityAvailable * b.sellingPrice),0), COALESCE(SUM(b.quantityAvailable),0)
            FROM InventoryBatch b
            WHERE b.quantityAvailable > 0 AND b.sellingPrice IS NOT NULL
            GROUP BY b.product.id
            """)
    List<Object[]> weightedAvgSellingPrices();

    // Total in-stock quantity per product (all available batches, regardless of price): [productId, SUM(available)].
    @Query("""
            SELECT b.product.id, COALESCE(SUM(b.quantityAvailable),0)
            FROM InventoryBatch b
            WHERE b.quantityAvailable > 0
            GROUP BY b.product.id
            """)
    List<Object[]> availableQtyByProduct();

    // Distinct in-stock batch selling prices per product (for the card price tooltip): [productId, sellingPrice].
    @Query("""
            SELECT DISTINCT b.product.id, b.sellingPrice
            FROM InventoryBatch b
            WHERE b.quantityAvailable > 0 AND b.sellingPrice IS NOT NULL
            ORDER BY b.product.id, b.sellingPrice
            """)
    List<Object[]> distinctInStockSalePrices();

    @Query("SELECT COALESCE(SUM(b.quantityReceived),0), COALESCE(SUM(b.quantityAvailable),0), COALESCE(SUM(b.quantityAvailable * b.unitCost),0) FROM InventoryBatch b WHERE b.product.id = :productId AND b.receivedDate BETWEEN :fromDate AND :toDate")
    List<Object[]> aggregateByProductIdAndReceivedDateBetween(@Param("productId") UUID productId, @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    // Per-source totals on AVAILABLE stock: [source, availQty, availCost, availValue, soldQty] for the product cards.
    @Query("""
                SELECT b.source, COALESCE(SUM(b.quantityAvailable),0),
                       COALESCE(SUM(b.quantityAvailable * b.unitCost),0),
                       COALESCE(SUM(b.quantityAvailable * COALESCE(b.sellingPrice, :fallbackSp)),0),
                       COALESCE(SUM(b.quantityReceived - b.quantityAvailable),0)
                FROM InventoryBatch b WHERE b.product.id = :productId GROUP BY b.source
            """)
    List<Object[]> aggregateByProductIdGroupedBySource(@Param("productId") UUID productId, @Param("fallbackSp") BigDecimal fallbackSp);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM InventoryBatch b
            JOIN FETCH b.product
            WHERE b.product.id = :productId
            AND b.quantityAvailable > 0
            AND b.status = com.skr.erp.common.constants.InventoryBatchStatus.ACTIVE
            ORDER BY b.receivedDate ASC
            """)
    List<InventoryBatch> findAvailableBatchesForSale(UUID productId);

    // Same FIFO list without the write lock — safe for read-only previews (sale form pricing).
    @Query("""
            SELECT b
            FROM InventoryBatch b
            JOIN FETCH b.product
            WHERE b.product.id = :productId
            AND b.quantityAvailable > 0
            AND b.status = com.skr.erp.common.constants.InventoryBatchStatus.ACTIVE
            ORDER BY b.receivedDate ASC
            """)
    List<InventoryBatch> findAvailableBatchesForSalePreview(UUID productId);

    @Query("""
            select b
            from InventoryBatch b
            join fetch b.product p
            order by p.name
            """)
    List<InventoryBatch> findAllBatchesWithProduct();

    @Query("""
            SELECT COALESCE(SUM(b.quantityAvailable), 0),
                   COALESCE(SUM(b.quantityAvailable * b.unitCost), 0)
            FROM InventoryBatch b
            WHERE b.status = com.skr.erp.common.constants.InventoryBatchStatus.ACTIVE
            """)
    List<Object[]> aggregateActiveInventory();
}
