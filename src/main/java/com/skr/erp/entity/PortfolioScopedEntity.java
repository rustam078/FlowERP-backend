package com.skr.erp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

/**
 * Base for every business record. Hibernate fills portfolio_id on insert from the signed-in
 * portfolio and adds "portfolio_id = current portfolio" to every query, so one portfolio can
 * never read or change another's data (see PortfolioTenantResolver).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class PortfolioScopedEntity extends BaseEntity {

    @TenantId
    @Column(name = "portfolio_id", nullable = false, updatable = false)
    private UUID portfolioId;
}
