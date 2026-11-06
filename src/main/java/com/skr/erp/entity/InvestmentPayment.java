package com.skr.erp.entity;

import com.skr.erp.common.constants.PaymentMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single payment made against an investment invoice (date + mode + amount).
 */
@Entity
@Table(name = "investment_payment")
@Getter
@Setter
public class InvestmentPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_id", nullable = false)
    private Investment investment;

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

    @Column(name = "reference", length = 120)
    private String reference;
}
