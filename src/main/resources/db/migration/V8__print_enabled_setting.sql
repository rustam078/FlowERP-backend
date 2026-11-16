-- Print buttons are hidden everywhere until an admin turns them on (Settings → Company & Invoice).
INSERT INTO system_setting (id, setting_key, setting_value, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'PRINT_ENABLED', 'false', 'Show Print buttons (invoices, reports, pay-slips): true / false', now(), now())
ON CONFLICT (setting_key) DO NOTHING;
