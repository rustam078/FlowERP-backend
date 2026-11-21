package com.skr.erp.audit;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the row changes of the running write request. Changes are buffered per transaction and
 * kept only if that transaction commits, so a rolled-back save never shows up as "changed".
 */
final class AuditChangeCollector {

    private static final ThreadLocal<List<EntityChange>> REQUEST = new ThreadLocal<>();

    private AuditChangeCollector() {
    }

    static void start() {
        REQUEST.set(new ArrayList<>());
    }

    // Committed changes of the request (empty if none); also ends collection on this thread.
    static List<EntityChange> finish() {
        List<EntityChange> changes = REQUEST.get();
        REQUEST.remove();
        return changes != null ? changes : List.of();
    }

    static boolean isActive() {
        return REQUEST.get() != null;
    }

    static void record(EntityChange change) {
        List<EntityChange> request = REQUEST.get();
        if (request == null) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            request.add(change);
            return;
        }
        currentBuffer(request).pending.add(change);
    }

    // The buffer of the current transaction (a REQUIRES_NEW transaction gets its own one).
    private static TxBuffer currentBuffer(List<EntityChange> request) {
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            if (sync instanceof TxBuffer buffer && buffer.request == request) return buffer;
        }
        TxBuffer buffer = new TxBuffer(request);
        TransactionSynchronizationManager.registerSynchronization(buffer);
        return buffer;
    }

    private static final class TxBuffer implements TransactionSynchronization {
        private final List<EntityChange> request;
        private final List<EntityChange> pending = new ArrayList<>();

        private TxBuffer(List<EntityChange> request) {
            this.request = request;
        }

        @Override
        public void afterCompletion(int status) {
            if (status == STATUS_COMMITTED) request.addAll(pending);
        }
    }
}
