package com.skr.erp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** A client/tenant. Every business record belongs to exactly one portfolio. */
@Entity
@Table(name = "portfolio")
@Getter
@Setter
public class Portfolio extends BaseEntity {

    // The default portfolio seeded by V1 (holds the template settings; cannot be deleted).
    public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Column(nullable = false, length = 120)
    private String name;

    // 2 capital letters picked at creation; unique and locked (batch numbers IL001, IL002 ...).
    @Column(name = "batch_prefix", nullable = false, length = 2, updatable = false)
    private String batchPrefix;

    // Last batch number used; advanced only by PortfolioRepository.nextBatchNumber.
    @Column(name = "batch_seq", nullable = false, insertable = false, updatable = false)
    private Long batchSeq;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    // Deleted portfolios are hidden and locked, but their data is kept (restorable).
    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

    // Usable = can be signed into / switched into.
    public boolean isUsable() {
        return Boolean.TRUE.equals(active) && !Boolean.TRUE.equals(deleted);
    }

    public boolean isDefault() {
        return DEFAULT_ID.equals(getId());
    }
}
