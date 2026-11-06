package com.skr.erp.entity;

import com.skr.erp.common.constants.InvestmentItemType;
import com.skr.erp.common.constants.UnitType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "investment_item")
@Getter
@Setter
public class InvestmentItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_id", nullable = false)
    private Investment investment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestmentItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitType unit;

    @Column(nullable = false)
    private BigDecimal rate;

    // Optional selling price (MRP) for this purchased lot; null → use the product's default price.
    @Column(name = "selling_price")
    private BigDecimal sellingPrice;

    @Column(nullable = false)
    private BigDecimal totalAmount;
}