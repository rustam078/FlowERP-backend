package com.skr.erp.entity;

import com.skr.erp.common.constants.PaymentMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single payment received against a sale (date + mode + amount).
 */
@Entity
@Table(name = "customer_payment")
@Getter
@Setter
public class CustomerPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMode mode;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    // Same id across the rows created by one payment (so an allocation can be traced).
    @Column(name = "payment_group_id")
    private UUID paymentGroupId;

    // Invoice no whose checkout made this payment, when it cleared a different (older) invoice.
    @Column(name = "reference_invoice_no", length = 50)
    private String referenceInvoiceNo;
}
