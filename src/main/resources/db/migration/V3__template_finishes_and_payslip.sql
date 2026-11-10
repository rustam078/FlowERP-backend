-- Post-baseline template finishes (folds the former template/setting migrations):
-- keep amounts on one line, drop the retired Other Charges row, add the payroll auto-settle
-- setting, and seed the pay-slip PDF template (modern black & white).

-- Amounts never wrap in any PDF template (symbol + number stay on one line)
UPDATE system_setting SET setting_value = REPLACE(setting_value, '.right { text-align: right; }', '.right { text-align: right; white-space: nowrap; }'), updated_at = now() WHERE setting_value LIKE '%.right { text-align: right; }%';
UPDATE system_setting SET setting_value = REPLACE(setting_value, '.num { text-align: right; }', '.num { text-align: right; white-space: nowrap; }'), updated_at = now() WHERE setting_value LIKE '%.num { text-align: right; }%';
UPDATE system_setting SET setting_value = REPLACE(setting_value, '.rt { text-align: right; }', '.rt { text-align: right; white-space: nowrap; }'), updated_at = now() WHERE setting_value LIKE '%.rt { text-align: right; }%';

-- Remove the retired "Other Charges" row from the purchase invoice template
UPDATE system_setting SET setting_value = REPLACE(setting_value, '<tr><td class="k">Other Charges</td><td class="right">{{otherCharge}}</td></tr>', ''), updated_at = now() WHERE setting_key = 'PURCHASE_INVOICE_TEMPLATE';

-- Auto-settle completed months from advance when enabled (admin settles manually otherwise)
INSERT INTO system_setting (id, setting_key, setting_value, created_at, updated_at)
SELECT gen_random_uuid(), 'PAYROLL_AUTO_SETTLE', 'false', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM system_setting WHERE setting_key = 'PAYROLL_AUTO_SETTLE');

-- Pay-slip PDF template (one template for monthly salary slips and piece-work payment slips)
INSERT INTO system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES
('c2a2d3b0-2222-4b22-9b02-00000000e2b1', 'PAYSLIP_TEMPLATE', '
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page {
    size: A4;
    margin: 36px;
  }

  body {
    font-family: Helvetica, Arial, sans-serif;
    color: #111111;
    font-size: 12px;
  }

  .sheet {
    border: 1px solid #d9d9d9;
    padding: 22px 24px;
  }

  table {
    border-collapse: collapse;
    width: 100%;
  }

  .hdr td {
    vertical-align: top;
    padding: 0;
  }

  .hdr .right {
    text-align: right;
  }

  .company {
    font-size: 19px;
    font-weight: bold;
    letter-spacing: 0.3px;
  }

  .muted {
    color: #666666;
    font-size: 10px;
    margin-top: 2px;
  }

  .title {
    font-size: 15px;
    font-weight: bold;
    letter-spacing: 2px;
    text-transform: uppercase;
  }

  .period {
    color: #444444;
    font-size: 11px;
    margin-top: 3px;
  }

  .subperiod {
    color: #777777;
    font-size: 10px;
    margin-top: 2px;
  }

  .rule {
    border: none;
    border-top: 2px solid #111111;
    margin: 12px 0 0;
  }

  .rule-soft {
    border: none;
    border-top: 1px solid #e5e5e5;
    margin: 14px 0;
  }

  /* Employee Information */
  .info {
    width: auto;
    table-layout: auto;
    margin-top: 14px;
  }

  .info td {
    padding: 5px 25px 5px 0;
    vertical-align: middle;
    white-space: nowrap;
  }

  .info .k {
    color: #888888;
    font-size: 10px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-right: 7px;
  }

  .info .v {
    font-weight: bold;
    font-size: 12px;
  }

  .items {
    margin-top: 6px;
  }

  .items th {
    text-align: left;
    font-size: 10px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: #555555;
    background: #f3f3f3;
    border-bottom: 1px solid #dddddd;
    padding: 8px 10px;
  }

  .items td {
    padding: 8px 10px;
    border-bottom: 1px solid #f0f0f0;
  }

  .items .num {
    text-align: right;
    white-space: nowrap;
  }

  .items .net td {
    border-bottom: none;
    border-top: 1px solid #333333;
    background: #fafafa;
    font-weight: bold;
    font-size: 13px;
  }

  .meta .right {
    text-align: right;
    color: #333333;
    font-size: 11px;
    line-height: 1.7;
  }

  .sign {
    margin-top: 42px;
  }

  .sign td {
    font-size: 11px;
    color: #444444;
  }

  .sign .right {
    text-align: right;
  }

  .foot {
    text-align: center;
    color: #999999;
    font-size: 9px;
    margin-top: 22px;
  }
</style>
</head>

<body>
  <div class="sheet">

    <!-- Header -->
    <table class="hdr">
      <tr>
        <td>
          <div class="company">{{companyName}}</div>
          <div class="muted">{{companyAddress}}</div>
          <div class="muted">{{companyContact}} &#183; GSTIN {{companyGstin}}</div>
        </td>

        <td class="right">
          <div class="title">{{title}}</div>
          <div class="period">{{periodLabel}}</div>
          <div class="subperiod">Settlement Date: {{settlementDate}}</div>
        </td>
      </tr>
    </table>

    <hr class="rule"/>

    <!-- Employee Information: 2 Columns x 2 Rows -->
    <table class="info">
      <tr>
        <td>
          <span class="k">Employee</span>
          <span class="v">{{employeeName}}</span>
        </td>

        <td>
          <span class="k">Code</span>
          <span class="v">{{employeeCode}}</span>
        </td>
      </tr>

      <tr>
        <td>
          <span class="k">Joining Date</span>
          <span class="v">{{joiningDate}}</span>
        </td>

        <td>
          <span class="k">Remaining Advance</span>
          <span class="v">{{remainingAdvance}}</span>
        </td>
      </tr>
    </table>

    <hr class="rule-soft"/>

    <!-- Settlement Details -->
    <table class="items">
      <tr>
        <th>Description</th>
        <th class="num">Amount</th>
      </tr>

      {{rows}}

      <tr class="net">
        <td>{{netLabel}}</td>
        <td class="num">{{netAmount}}</td>
      </tr>
    </table>

    <!-- Payment Information -->
    <table class="meta" style="margin-top:12px;">
      <tr>
        <td class="right">
          <span class="k">Paid Via</span>
          <span class="v">{{paidVia}}</span>
          <br/>
          <span class="k">Status</span>
          <span class="v">{{status}}</span>
        </td>
      </tr>
    </table>

    <!-- Signatures -->
    <table class="sign">
      <tr>
        <td>
          _______________________<br/>
          Employee Signature
        </td>

        <td class="right">
          _______________________<br/>
          Authorised Signatory
        </td>
      </tr>
    </table>

    <!-- Footer -->
    <div class="foot">
      Generated on {{generatedOn}} &#183; Computer-generated pay-slip, no signature required.
    </div>

  </div>
</body>
</html>
', 'Pay-slip PDF template (salary / piece-work payment)', NOW(), NOW());

