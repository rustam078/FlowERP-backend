package com.skr.erp.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AuditLogChangeRepository extends JpaRepository<AuditLogChange, UUID> {

    List<AuditLogChange> findByAuditLogIdOrderByCreatedAtAsc(UUID auditLogId);

    // Number of changed rows per log, for one page of the list in a single query.
    @Query("SELECT c.auditLogId, COUNT(c) FROM AuditLogChange c WHERE c.auditLogId IN :ids GROUP BY c.auditLogId")
    List<Object[]> countByAuditLogIds(@Param("ids") Collection<UUID> ids);
}
