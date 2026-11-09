-- Work report header: make the header table span the full page width so the right-hand
-- block (Work Report title + employee/report details) sits at the extreme right edge,
-- opposite the left-aligned company details, instead of floating near the centre.

UPDATE system_setting
SET setting_value = REPLACE(setting_value,
        '.hdr td { vertical-align: top; padding: 0; width: 50%; }',
        'table.hdr { width: 100%; }
  .hdr td { vertical-align: top; padding: 0; width: 50%; }'),
    updated_at = now()
WHERE setting_key IN ('PRODUCTION_PDF_TEMPLATE', 'PRODUCTION_REPORT_TEMPLATE');
