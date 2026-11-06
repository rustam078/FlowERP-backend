package com.skr.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "production_entry_detail")
@Getter
@Setter
public class ProductionEntryDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_entry_id", nullable = false)
    private ProductionEntry productionEntry;

    // The piece-work task (e.g. "Cutting"). Free text; the deal is locked per entry.
    @Column(name = "work")
    private String work;

    // Optional link to a product (reporting only). Piece-work is not tied to a final product.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "rate_snapshot")
    private BigDecimal rateSnapshot;

    @Column(name = "amount_snapshot")
    private BigDecimal amountSnapshot;

    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "piece_code_id")
    private ProductPieceCode pieceCode;

    @Column(name = "piece_code_snapshot")
    private String pieceCodeSnapshot;

    @Column(name = "inventory_batch_id")
    private UUID inventoryBatchId;
}