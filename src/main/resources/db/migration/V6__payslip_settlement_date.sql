-- Show the settlement date (when the salary/piece work was cleared) under the period on the pay-slip.
UPDATE system_setting
SET setting_value = REPLACE(setting_value,
        '.period { color: #444444; font-size: 11px; margin-top: 3px; }',
        '.period { color: #444444; font-size: 11px; margin-top: 3px; }
  .subperiod { color: #777777; font-size: 10px; margin-top: 2px; }'),
    updated_at = now()
WHERE setting_key = 'PAYSLIP_TEMPLATE';

UPDATE system_setting
SET setting_value = REPLACE(setting_value,
        '<div class="period">{{periodLabel}}</div>',
        '<div class="period">{{periodLabel}}</div>
          <div class="subperiod">Settlement Date: {{settlementDate}}</div>'),
    updated_at = now()
WHERE setting_key = 'PAYSLIP_TEMPLATE';
