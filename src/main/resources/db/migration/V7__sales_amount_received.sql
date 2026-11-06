-- Store how much the customer actually paid at this sale's checkout (immutable).
-- amount_paid can still grow later when a newer sale's payment clears this one's due,
-- but amount_received stays what was handed over at this checkout.
ALTER TABLE sales_order ADD COLUMN IF NOT EXISTS amount_received numeric(19, 2) NOT NULL DEFAULT 0;

-- Existing sales were fully paid at their own checkout.
UPDATE sales_order SET amount_received = grand_total;
