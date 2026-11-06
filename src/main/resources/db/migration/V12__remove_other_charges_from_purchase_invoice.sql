-- "Other Charges" is no longer used; drop its row from the purchase invoice PDF template.
UPDATE system_setting
SET setting_value = REPLACE(setting_value,
        '<tr><td class="k">Other Charges</td><td class="right">{{otherCharge}}</td></tr>', ''),
    updated_at = now()
WHERE setting_key = 'PURCHASE_INVOICE_TEMPLATE';
