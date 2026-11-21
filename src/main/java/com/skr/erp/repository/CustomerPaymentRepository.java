package com.skr.erp.repository;

import com.skr.erp.common.constants.PaymentRules;
import com.skr.erp.entity.CustomerPayment;
import com.skr.erp.security.RequestContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CustomerPaymentRepository extends JpaRepository<CustomerPayment, UUID> {

    List<CustomerPayment> findBySalesOrderIdOrderByCreatedAtDesc(UUID salesOrderId);

    // Total received across whole payments (one group = one cashier payment that may span sales).
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM CustomerPayment p WHERE p.paymentGroupId IN :groupIds")
    BigDecimal sumByGroupIds(@Param("groupIds") Collection<UUID> groupIds);

    // All of a customer's payments (newest first) with their sale, for the payment history.
    @Query("SELECT p FROM CustomerPayment p JOIN FETCH p.salesOrder s WHERE s.customerMobile = :mobile ORDER BY p.createdAt DESC")
    List<CustomerPayment> findByCustomerMobile(@Param("mobile") String mobile);

    // Payments made at one invoice's checkout that cleared OTHER (older) invoices — the previous-due breakdown.
    @Query("SELECT p FROM CustomerPayment p JOIN FETCH p.salesOrder s WHERE p.referenceInvoiceNo = :invoiceNo ORDER BY s.createdAt ASC")
    List<CustomerPayment> findByReferenceInvoiceNo(@Param("invoiceNo") String invoiceNo);

    // Rows of one payment event (oldest invoice first) — used to answer a repeated settlement request.
    @Query("SELECT p FROM CustomerPayment p JOIN FETCH p.salesOrder s WHERE p.paymentGroupId = :groupId ORDER BY s.createdAt ASC")
    List<CustomerPayment> findByPaymentGroupId(@Param("groupId") UUID groupId);

    // Cash from due settlements (payments made after the invoice's own checkout, not via a later sale) in a period.
    @Query(value = """
                SELECT COALESCE(SUM(p.amount), 0) FROM customer_payment p JOIN sales_order s ON s.id = p.sales_order_id
                WHERE p.portfolio_id =""" + RequestContext.PORTFOLIO_SQL_PARAM + """
                  AND p.reference_invoice_no IS NULL
                  AND """ + PaymentRules.PAID_AFTER_CHECKOUT_SQL + """
                  AND (CAST(:start AS timestamp) IS NULL OR p.created_at >= CAST(:start AS timestamp))
                  AND (CAST(:end AS timestamp) IS NULL OR p.created_at < CAST(:end AS timestamp))
                  AND (CAST(:search AS text) IS NULL OR LOWER(s.invoice_no) LIKE CAST(:search AS text)
                       OR LOWER(s.customer_name) LIKE CAST(:search AS text) OR LOWER(s.customer_mobile) LIKE CAST(:search AS text))
                  AND (CAST(:mode AS text) IS NULL OR p.mode = CAST(:mode AS text))
                  AND (CAST(:status AS text) IS NULL OR s.payment_status = CAST(:status AS text))
            """, nativeQuery = true)
    BigDecimal sumSettlements(@Param("search") String search, @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end, @Param("mode") String mode, @Param("status") String status);
}
