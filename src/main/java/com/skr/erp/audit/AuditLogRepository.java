package com.skr.erp.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    // Retention: removes up to `batch` logs older than the cutoff (their changes go with them, FK cascade).
    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM audit_log
            WHERE id IN (SELECT id FROM audit_log WHERE created_at < :cutoff LIMIT :batch)
            """, nativeQuery = true)
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff, @Param("batch") int batch);
}
