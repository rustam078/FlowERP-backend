-- Step 2: Production becomes employee piece-work (labor/payroll), decoupled from inventory + piece codes.
-- Add a free-text work/task column; a product link is now optional (reporting only).
ALTER TABLE production_entry_detail ADD COLUMN IF NOT EXISTS work VARCHAR(255);
ALTER TABLE production_entry_detail ALTER COLUMN product_id DROP NOT NULL;
