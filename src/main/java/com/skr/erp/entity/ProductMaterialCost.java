package com.skr.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "product_material_cost")
@Getter
@Setter
public class ProductMaterialCost extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private BigDecimal cost;

    // Selling price effective from the same date. Nullable for legacy rows
    // created before pricing history tracked a sale price.
    @Column(name = "sale_price")
    private BigDecimal salePrice;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}