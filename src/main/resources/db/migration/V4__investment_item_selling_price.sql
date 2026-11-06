-- MRP / selling price captured per purchased item (used as the stock batch's selling price).
ALTER TABLE investment_item ADD COLUMN IF NOT EXISTS selling_price numeric(14, 2);
