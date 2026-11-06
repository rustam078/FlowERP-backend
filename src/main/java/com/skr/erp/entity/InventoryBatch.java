package com.skr.erp.entity;

import com.skr.erp.common.constants.InventoryBatchStatus;
import com.skr.erp.common.constants.ProductSource;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "inventory_batch")
@Data
public class InventoryBatch extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(nullable = false)
    private UUID sourceId;

    @Column(nullable = false)
    private LocalDate receivedDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityReceived;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityAvailable;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryBatchStatus status;

    @Column(length = 500)
    private String remarks;

}