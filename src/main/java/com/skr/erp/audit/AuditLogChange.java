package com.skr.erp.audit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** One database row changed by an audited request, with its exact state before and after. */
@Entity
@Table(name = "audit_log_change")
@Getter
@Setter
public class AuditLogChange {

    @Id
    private UUID id;

    @Column(name = "audit_log_id", nullable = false, updatable = false)
    private UUID auditLogId;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "entity_name", nullable = false, length = 80)
    private String entityName;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(nullable = false, length = 10)
    private String operation; // INSERT / UPDATE / DELETE

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state")
    private JsonNode beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state")
    private JsonNode afterState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changed_fields")
    private List<String> changedFields;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
