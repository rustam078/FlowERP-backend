package com.skr.erp.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skr.erp.entity.AppUser;
import com.skr.erp.entity.BaseEntity;
import com.skr.erp.entity.Portfolio;
import com.skr.erp.entity.PortfolioScopedEntity;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Records the exact before/after values of every business row inserted, updated or deleted during an
 * audited request. Generic: works for every entity without per-table code.
 */
@Component
@RequiredArgsConstructor
public class EntityChangeListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static final Set<String> SECRET_FIELDS = Set.of("password", "tokenHash");

    private final EntityManagerFactory entityManagerFactory;
    private final ObjectMapper objectMapper;

    @PostConstruct
    void register() {
        EventListenerRegistry registry = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry().getService(EventListenerRegistry.class);
        registry.appendListeners(EventType.POST_INSERT, this);
        registry.appendListeners(EventType.POST_UPDATE, this);
        registry.appendListeners(EventType.POST_DELETE, this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (!tracked(event.getEntity())) return;
        record(event.getEntity(), "INSERT", null, toJson(event.getPersister(), event.getState()), null);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (!tracked(event.getEntity())) return;
        EntityPersister persister = event.getPersister();
        JsonNode before = event.getOldState() != null ? toJson(persister, event.getOldState()) : null;
        record(event.getEntity(), "UPDATE", before, toJson(persister, event.getState()), changedFields(event));
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (!tracked(event.getEntity())) return;
        record(event.getEntity(), "DELETE", toJson(event.getPersister(), event.getDeletedState()), null, null);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    // Business rows plus users and portfolios; never sessions or the audit tables themselves.
    private static boolean tracked(Object entity) {
        return AuditChangeCollector.isActive()
                && (entity instanceof PortfolioScopedEntity || entity instanceof AppUser || entity instanceof Portfolio);
    }

    private void record(Object entity, String operation, JsonNode before, JsonNode after, List<String> changed) {
        BaseEntity base = (BaseEntity) entity;
        UUID portfolioId = entity instanceof PortfolioScopedEntity scoped ? scoped.getPortfolioId() : null;
        AuditChangeCollector.record(new EntityChange(portfolioId, entity.getClass().getSimpleName(), base.getId(),
                operation, before, after, changed, LocalDateTime.now()));
    }

    private static List<String> changedFields(PostUpdateEvent event) {
        String[] names = event.getPersister().getPropertyNames();
        List<String> changed = new ArrayList<>();
        if (event.getDirtyProperties() != null) {
            for (int index : event.getDirtyProperties()) changed.add(names[index]);
        }
        return changed;
    }

    // Column values as JSON: related rows by id, collections skipped, secrets masked, bytes summarised.
    private JsonNode toJson(EntityPersister persister, Object[] state) {
        String[] names = persister.getPropertyNames();
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < names.length && i < state.length; i++) {
            Object value = state[i];
            if (value instanceof PersistentCollection<?> || value instanceof java.util.Collection<?>) continue;
            values.put(names[i], SECRET_FIELDS.contains(names[i]) ? "***" : simplify(value));
        }
        return objectMapper.valueToTree(values);
    }

    private static Object simplify(Object value) {
        if (value instanceof BaseEntity related) return related.getId();
        if (value instanceof byte[] bytes) return "[" + bytes.length + " bytes]";
        if (value instanceof Enum<?> e) return e.name();
        return value;
    }
}
