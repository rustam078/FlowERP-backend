package com.skr.erp.security;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Tells Hibernate which portfolio the current request works in (from the session token).
 * With no portfolio (login, a SUPER_ADMIN on the picker) it answers a portfolio that owns
 * nothing, so business queries return no rows instead of everyone's rows.
 */
@Component
public class PortfolioTenantResolver implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    static final UUID NO_PORTFOLIO = new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        UUID portfolioId = RequestContext.portfolioIdOrNull();
        return portfolioId != null ? portfolioId : NO_PORTFOLIO;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
