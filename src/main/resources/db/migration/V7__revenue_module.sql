-- Revenue module: sidebar entry just above Settings, and a STAFF permission row (off by default; ADMIN always has access).
UPDATE system_setting
SET setting_value = replace(setting_value, '"settings"', '"revenue","settings"'), updated_at = now()
WHERE setting_key = 'MENU_ORDER' AND setting_value NOT LIKE '%"revenue"%';

UPDATE system_setting
SET setting_value = jsonb_set(setting_value::jsonb, '{STAFF,revenue}', '{"view":false,"write":false,"delete":false}'::jsonb, true)::text,
    updated_at = now()
WHERE setting_key = 'ROLE_PERMISSIONS' AND setting_value::jsonb ? 'STAFF' AND NOT (setting_value::jsonb -> 'STAFF') ? 'revenue';

-- Date indexes so the newest-first Revenue list can read each payment table in order.
CREATE INDEX IF NOT EXISTS idx_customer_payment_date ON customer_payment (payment_date, created_at);
CREATE INDEX IF NOT EXISTS idx_investment_payment_date ON investment_payment (payment_date, created_at);
CREATE INDEX IF NOT EXISTS idx_ledger_entry_date ON employee_ledger (entry_date, created_at);
