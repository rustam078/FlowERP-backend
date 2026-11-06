package com.skr.erp.entity;

import com.skr.erp.common.constants.InvestmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "investment")
@Getter
@Setter
public class Investment extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private InvestmentType investmentType;

    @Column(nullable = false)
    private LocalDate purchaseDate;


    @Column(nullable = false)
    private BigDecimal subTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal otherCharge = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "investment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvestmentItem> items = new ArrayList<>();
}