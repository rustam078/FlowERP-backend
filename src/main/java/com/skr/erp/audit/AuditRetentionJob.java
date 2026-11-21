package com.skr.erp.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

/**
 * Keeps only the last N days (default 60) of audit logs: runs every night at 02:30 and once at start-up
 * (the desktop PC may be off at night). Deletes in small batches so tables are never locked for long.
 */
@Configuration
@EnableScheduling
public class AuditRetentionJob {

    private static final int BATCH = 5000;
    private static final Logger log = LoggerFactory.getLogger(AuditRetentionJob.class);

    private final AuditLogRepository auditLogRepository;
    private final int retentionDays;

    public AuditRetentionJob(AuditLogRepository auditLogRepository, @Value("${audit.retention-days:60}") int retentionDays) {
        this.auditLogRepository = auditLogRepository;
        this.retentionDays = retentionDays;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        purge();
    }

    @Scheduled(cron = "0 30 2 * * *")
    public void nightly() {
        purge();
    }

    void purge() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int total = 0;
        int deleted;
        do {
            deleted = auditLogRepository.deleteOlderThan(cutoff, BATCH);
            total += deleted;
        } while (deleted == BATCH);
        if (total > 0) log.info("Audit retention: removed {} logs older than {} days", total, retentionDays);
    }
}
