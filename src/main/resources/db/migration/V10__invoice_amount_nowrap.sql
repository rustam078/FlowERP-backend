-- Keep the currency symbol + amount on one line in every PDF template.
-- Large amounts (6-7 digits) were wrapping, pushing the symbol above the number.
UPDATE system_setting
SET setting_value = REPLACE(setting_value, '.right { text-align: right; }', '.right { text-align: right; white-space: nowrap; }'),
    updated_at = now()
WHERE setting_value LIKE '%.right { text-align: right; }%';

UPDATE system_setting
SET setting_value = REPLACE(setting_value, '.num { text-align: right; }', '.num { text-align: right; white-space: nowrap; }'),
    updated_at = now()
WHERE setting_value LIKE '%.num { text-align: right; }%';

UPDATE system_setting
SET setting_value = REPLACE(setting_value, '.rt { text-align: right; }', '.rt { text-align: right; white-space: nowrap; }'),
    updated_at = now()
WHERE setting_value LIKE '%.rt { text-align: right; }%';
