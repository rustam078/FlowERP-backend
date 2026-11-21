package com.skr.erp.repository;

import com.skr.erp.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    boolean existsByBatchPrefix(String batchPrefix);

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);

    // Super admin list (deleted ones hidden), optionally filtered by name or prefix.
    @Query("""
            SELECT p FROM Portfolio p
            WHERE p.deleted = false
              AND (:search IS NULL OR LOWER(p.name) LIKE :search OR LOWER(p.batchPrefix) LIKE :search)
            ORDER BY p.name
            """)
    List<Portfolio> search(@Param("search") String search);

    // Next batch number of a portfolio (IL001, IL002 ... IL1000), taken atomically: the row lock of
    // this UPDATE makes concurrent batch creations wait, so a number is never handed out twice.
    @Query(value = """
            UPDATE portfolio SET batch_seq = batch_seq + 1, updated_at = now()
            WHERE id = :portfolioId
            RETURNING batch_prefix || CASE WHEN batch_seq < 1000 THEN lpad(CAST(batch_seq AS text), 3, '0')
                                           ELSE CAST(batch_seq AS text) END
            """, nativeQuery = true)
    String nextBatchNumber(@Param("portfolioId") UUID portfolioId);

    // Next sale invoice counter of a portfolio (same atomic row-lock pattern).
    @Query(value = """
            UPDATE portfolio SET sale_invoice_seq = sale_invoice_seq + 1, updated_at = now()
            WHERE id = :portfolioId RETURNING sale_invoice_seq
            """, nativeQuery = true)
    long nextSaleInvoiceSeq(@Param("portfolioId") UUID portfolioId);

    // Next purchase (investment) invoice counter of a portfolio.
    @Query(value = """
            UPDATE portfolio SET purchase_invoice_seq = purchase_invoice_seq + 1, updated_at = now()
            WHERE id = :portfolioId RETURNING purchase_invoice_seq
            """, nativeQuery = true)
    long nextPurchaseInvoiceSeq(@Param("portfolioId") UUID portfolioId);
}
