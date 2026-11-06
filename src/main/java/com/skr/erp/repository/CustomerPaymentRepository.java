package com.skr.erp.repository;

import com.skr.erp.entity.CustomerPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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
}
