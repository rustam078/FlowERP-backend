package com.skr.erp.entity;

import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_order")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrder extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String invoiceNo;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerMobile;

    private String customerEmail;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tax;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal;

    // How much of this sale has been paid; the rest (grandTotal - amountPaid) is the customer's due.
    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountPaid;

    // What the customer actually handed over at THIS sale's checkout — immutable once set.
    @Column(name = "amount_received", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountReceived;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private String paymentProvider;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private String remarks;
}