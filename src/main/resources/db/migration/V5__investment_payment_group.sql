-- Groups the per-invoice rows created by one payment so a vendor payment can be traced.
ALTER TABLE investment_payment ADD COLUMN IF NOT EXISTS payment_group_id uuid;
