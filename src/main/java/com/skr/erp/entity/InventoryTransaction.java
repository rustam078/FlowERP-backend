package com.skr.erp.entity;

import com.skr.erp.common.constants.InventoryTransactionType;
import jakarta.persistence.*;
import lombok.Builder;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory_transaction")
@Builder
public class InventoryTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_item_id", nullable = false)
    private SalesOrderItem salesOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private InventoryBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryTransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    /** Free-text audit note, e.g. the source of a RECONCILIATION adjustment. */
    @Column(length = 200)
    private String reason;
}