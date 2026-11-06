package com.skr.erp.repository;

import com.skr.erp.entity.InvestmentPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InvestmentPaymentRepository extends JpaRepository<InvestmentPayment, UUID> {

    List<InvestmentPayment> findByInvestmentIdOrderByPaymentDateDescCreatedAtDesc(UUID investmentId);

    // Total paid toward investments whose purchase date falls in the range (for the expenses-due KPI).
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM InvestmentPayment p WHERE p.investment.purchaseDate >= :from AND p.investment.purchaseDate <= :to")
    BigDecimal paidForInvestmentsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM InvestmentPayment p WHERE p.investment.id = :id")
    BigDecimal sumByInvestment(@Param("id") UUID id);

    // Total paid per investment in one query (for the list view).
    @Query("SELECT p.investment.id, COALESCE(SUM(p.amount), 0) FROM InvestmentPayment p GROUP BY p.investment.id")
    List<Object[]> paidSums();

    // All payments for a vendor (newest first), with the invoice loaded for the history view.
    @Query("SELECT p FROM InvestmentPayment p JOIN FETCH p.investment i WHERE i.vendor.id = :vendorId ORDER BY p.createdAt DESC")
    List<InvestmentPayment> findByVendorId(@Param("vendorId") UUID vendorId);

    // Every payment belonging to the given payment groups, invoice loaded (for the allocation breakdown).
    @Query("SELECT p FROM InvestmentPayment p JOIN FETCH p.investment i WHERE p.paymentGroupId IN :groupIds ORDER BY i.purchaseDate ASC, p.createdAt ASC")
    List<InvestmentPayment> findByGroupIds(@Param("groupIds") List<UUID> groupIds);

    // Total amount actually received per transaction, attributed to the invoice the payment was made from
    // (the newest invoice in each payment group). Used for the "Amount Received" column in the list.
    @Query(value =
            "SELECT o.investment_id, g.total FROM " +
            "(SELECT COALESCE(payment_group_id, id) gid, SUM(amount) total FROM investment_payment GROUP BY COALESCE(payment_group_id, id)) g " +
            "JOIN (SELECT DISTINCT ON (COALESCE(p.payment_group_id, p.id)) COALESCE(p.payment_group_id, p.id) gid, p.investment_id " +
            "      FROM investment_payment p JOIN investment i ON i.id = p.investment_id " +
            "      ORDER BY COALESCE(p.payment_group_id, p.id), i.created_at DESC) o ON o.gid = g.gid",
            nativeQuery = true)
    List<Object[]> receivedByOriginInvoice();
}
