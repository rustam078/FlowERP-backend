-- Work report header polish: equal-width header columns so "Work Report" always shows on the
-- right, and the date-range report's company line split into separate address / contact rows.

UPDATE system_setting
SET setting_value = REPLACE(setting_value,
        '.hdr td { vertical-align: top; padding: 0; }',
        '.hdr td { vertical-align: top; padding: 0; width: 50%; }'),
    updated_at = now()
WHERE setting_key IN ('PRODUCTION_PDF_TEMPLATE', 'PRODUCTION_REPORT_TEMPLATE');

UPDATE system_setting
SET setting_value = REPLACE(setting_value,
        '<div class="muted">{{companyAddress}} | {{companyContact}} | GSTIN {{companyGstin}}</div>',
        '<div class="muted">{{companyAddress}}</div>
        <div class="muted">{{companyContact}} | GSTIN {{companyGstin}}</div>'),
    updated_at = now()
WHERE setting_key = 'PRODUCTION_REPORT_TEMPLATE';
