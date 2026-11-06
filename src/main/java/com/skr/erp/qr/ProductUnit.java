package com.skr.erp.qr;

import com.skr.erp.common.constants.ProductUnitStatus;
import com.skr.erp.entity.BaseEntity;
import com.skr.erp.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One physical garment: a scannable, serialized unit that belongs to an inventory
 * batch. Its {@code printedPrice} is frozen at generation time (what is printed on
 * the tag). Consumed exactly once by a sale (status SOLD).
 */
@Entity
@Table(name = "product_unit")
@Getter
@Setter
public class ProductUnit extends BaseEntity {

    /** e.g. {@code BT000087-001}. Unique across the system. */
    @Column(nullable = false, unique = true, length = 40)
    private String serial;

    /** The batch this unit was cut from (batch numbers are unique). */
    @Column(name = "batch_number", nullable = false, length = 40)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Selling price frozen onto the tag at generation time. */
    @Column(name = "printed_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal printedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductUnitStatus status = ProductUnitStatus.AVAILABLE;

    /** The sales order that consumed this unit (null while AVAILABLE). */
    @Column(name = "sale_order_id")
    private UUID saleOrderId;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;
}
