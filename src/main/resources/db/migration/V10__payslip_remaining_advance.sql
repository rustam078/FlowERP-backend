-- Pay-slip: show the employee's remaining (unadjusted) advance right after the Code field,
-- so every slip states how much advance the employee still owes.

UPDATE system_setting
SET setting_value = REPLACE(setting_value,
        '<td><span class="k">Code</span><span class="v">{{employeeCode}}</span></td>
      </tr>',
        '<td><span class="k">Code</span><span class="v">{{employeeCode}}</span></td>
      </tr>
      <tr>
        <td><span class="k">Remaining Advance</span><span class="v">{{remainingAdvance}}</span></td>
        <td></td>
      </tr>'),
    updated_at = now()
WHERE setting_key = 'PAYSLIP_TEMPLATE';
