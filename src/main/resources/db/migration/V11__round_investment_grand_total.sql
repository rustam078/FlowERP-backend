-- Final amount is always whole rupees now; round any existing investment grand totals that kept paise
-- (a 0.20 remainder was leaving fully-paid invoices stuck as PARTIALLY_PAID).
UPDATE investment
SET grand_total = ROUND(grand_total), updated_at = now()
WHERE grand_total <> ROUND(grand_total);
