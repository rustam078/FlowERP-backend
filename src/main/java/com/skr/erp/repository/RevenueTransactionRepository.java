package com.skr.erp.repository;

import com.skr.erp.common.constants.PaymentRules;
import com.skr.erp.dto.request.RevenueFilter;
import com.skr.erp.security.RequestContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Read-only view over every real money movement, taken live from each module's own payment table
 * (sales payments, investment payments, employee cash ledger). Nothing is copied, so edits and
 * deletions in a module show up here immediately. Two queries per request: one page + one total.
 */
@Repository
public class RevenueTransactionRepository {

    // One row per payment row. ADVANCE_ADJUSTMENT / EARNING_SALARY are book entries (no cash), so excluded.
    private static final String TX = """
            WITH tx AS (
                SELECT p.id, 'SALES' AS module,
                       CASE WHEN p.reference_invoice_no IS NOT NULL THEN 'DUE_CLEARED'
                            WHEN """ + PaymentRules.PAID_AFTER_CHECKOUT_SQL + """
                             THEN 'DUE_SETTLEMENT' ELSE 'SALE_PAYMENT' END AS type,
                       p.payment_date AS txn_date, p.created_at, s.invoice_no AS reference, p.reference_invoice_no AS note,
                       s.customer_name AS party, s.customer_mobile AS contact, p.mode, p.amount, 'IN' AS direction, s.id AS source_id
                FROM customer_payment p JOIN sales_order s ON s.id = p.sales_order_id
                WHERE p.portfolio_id = :portfolioId
                UNION ALL
                SELECT ip.id, 'INVESTMENT', i.investment_type, ip.payment_date, ip.created_at, i.invoice_number, ip.reference,
                       v.name, v.mobile, ip.mode, ip.amount, 'OUT', i.id
                FROM investment_payment ip JOIN investment i ON i.id = ip.investment_id LEFT JOIN vendor v ON v.id = i.vendor_id
                WHERE ip.portfolio_id = :portfolioId AND i.is_deleted = false
                UNION ALL
                SELECT l.id, 'EMPLOYEE', l.type, l.entry_date, l.created_at, e.employee_code, l.note,
                       e.full_name, e.mobile_number, l.mode, l.amount, 'OUT', e.id
                FROM employee_ledger l JOIN employee e ON e.id = l.employee_id
                WHERE l.portfolio_id = :portfolioId AND l.type IN ('ADVANCE', 'PAYOUT', 'SPEND')
            )
            """;

    private static final String WHERE = """
            WHERE (CAST(:module AS text) IS NULL OR tx.module = CAST(:module AS text))
              AND (CAST(:type AS text) IS NULL OR tx.type = CAST(:type AS text))
              AND (CAST(:mode AS text) IS NULL OR tx.mode = CAST(:mode AS text))
              AND (CAST(:fromDate AS date) IS NULL OR tx.txn_date >= CAST(:fromDate AS date))
              AND (CAST(:toDate AS date) IS NULL OR tx.txn_date <= CAST(:toDate AS date))
              AND (CAST(:search AS text) IS NULL OR LOWER(tx.reference) LIKE CAST(:search AS text)
                   OR LOWER(COALESCE(tx.party, '')) LIKE CAST(:search AS text)
                   OR COALESCE(tx.contact, '') LIKE CAST(:search AS text)
                   OR LOWER(COALESCE(tx.note, '')) LIKE CAST(:search AS text))
            """;

    private static final String PAGE_SQL = TX + "SELECT tx.* FROM tx " + WHERE
            + " ORDER BY tx.txn_date DESC, tx.created_at DESC, tx.id DESC LIMIT :limit OFFSET :offset";

    private static final String TOTALS_SQL = TX + """
            SELECT COUNT(*) AS cnt,
                   COALESCE(SUM(CASE WHEN tx.direction = 'IN' THEN tx.amount END), 0) AS inflow,
                   COALESCE(SUM(CASE WHEN tx.direction = 'OUT' THEN tx.amount END), 0) AS outflow
            FROM tx
            """ + WHERE;

    @PersistenceContext
    private EntityManager entityManager;

    // One page of transactions, newest first.
    @SuppressWarnings("unchecked")
    public List<Tuple> findPage(RevenueFilter filter, int limit, long offset) {
        NativeQuery<Tuple> query = nativeQuery(PAGE_SQL, filter);
        query.setParameter("limit", limit, Integer.class);
        query.setParameter("offset", offset, Long.class);
        return query.getResultList();
    }

    // Count + money in + money out for the same filter (drives pagination and the header totals).
    public Tuple totals(RevenueFilter filter) {
        return nativeQuery(TOTALS_SQL, filter).getSingleResult();
    }

    // Typed binding so empty filters reach PostgreSQL as typed NULLs.
    @SuppressWarnings("unchecked")
    private NativeQuery<Tuple> nativeQuery(String sql, RevenueFilter f) {
        NativeQuery<Tuple> query = entityManager.createNativeQuery(sql, Tuple.class).unwrap(NativeQuery.class);
        query.setParameter("portfolioId", RequestContext.portfolioId(), UUID.class); // signed-in portfolio only
        query.setParameter("module", f.getModule() != null ? f.getModule().name() : null, String.class);
        query.setParameter("type", blankToNull(f.getType()), String.class);
        query.setParameter("mode", f.getPaymentMode() != null ? f.getPaymentMode().name() : null, String.class);
        query.setParameter("fromDate", f.getFromDate(), LocalDate.class);
        query.setParameter("toDate", f.getToDate(), LocalDate.class);
        String search = blankToNull(f.getSearch());
        query.setParameter("search", search != null ? "%" + search.toLowerCase() + "%" : null, String.class);
        return query;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
