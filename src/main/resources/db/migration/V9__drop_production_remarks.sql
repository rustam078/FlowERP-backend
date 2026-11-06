-- Remarks removed from production everywhere (form, API, DB).
ALTER TABLE production_entry DROP COLUMN IF EXISTS remarks;
