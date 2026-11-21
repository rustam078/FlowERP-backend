-- Per-portfolio settings (company profile, invoice/report templates, menu, permissions, toggles).

CREATE TABLE system_setting (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    setting_key varchar(100) NOT NULL,
    setting_value text NOT NULL,
    description varchar(500),
    created_at timestamp,
    updated_at timestamp,
    CONSTRAINT system_setting_pkey PRIMARY KEY (id),
    CONSTRAINT uk_system_setting_key UNIQUE (portfolio_id, setting_key),
    CONSTRAINT fk_system_setting_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);

-- Default settings of the ILITY portfolio.
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('6d03d7d2-f735-4a96-bbbd-1718fb217221', '00000000-0000-0000-0000-000000000001', 'COMPANY_ADDRESS', '14 Loom Street, Industrial Area, Patna, Bihar 800001', 'Company address shown on invoices', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('cf220e2a-27b6-4e8e-8033-5f846ae58b6b', '00000000-0000-0000-0000-000000000001', 'COMPANY_CONTACT', '+91 90000 12345', 'Company phone / email shown on invoices', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('5e11a702-f8d0-4328-b137-85a191279cb2', '00000000-0000-0000-0000-000000000001', 'COMPANY_GSTIN', '10ABCDE1234F1Z5', 'Company GST identification number', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('bc1b9621-13e6-47de-ba08-08dc613a272f', '00000000-0000-0000-0000-000000000001', 'COMPANY_NAME', 'SKR Garment', 'Company name shown on invoices and headers', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('aeef3a15-08de-466b-bf45-79a5d23c81f8', '00000000-0000-0000-0000-000000000001', 'CURRENCY_SYMBOL', '₹', 'Currency symbol used across the app', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('a5ac9cd5-50cf-4657-9828-7c5b832fc968', '00000000-0000-0000-0000-000000000001', 'DEFAULT_GST_PERCENT', '5', 'Default GST percent applied to new invoices (snapshotted per invoice)', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('b3f3c2a0-3333-4a33-9a03-00000000d3a3', '00000000-0000-0000-0000-000000000001', 'EMPLOYEE_REPORT_TEMPLATE', '<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page { size: A4; margin: 24px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #000; font-size: 11px; }
  .hwrap { width: 100%; border-collapse: collapse; border-bottom: 2px solid #000; }
  .hwrap td { vertical-align: top; padding: 0 0 9px; }
  .company { font-size: 18px; font-weight: bold; letter-spacing: 0.3px; }
  .cdetail { font-size: 9.5px; color: #333; margin-top: 3px; line-height: 1.5; }
  .rt { text-align: right; white-space: nowrap; }
  .rt .t { font-size: 15px; font-weight: bold; text-transform: uppercase; letter-spacing: 1px; }
  .rt .p { font-size: 10px; margin-top: 3px; }
  .empbar { width: 100%; border-collapse: collapse; border: 1px solid #000; margin-top: 12px; }
  .empbar td { padding: 9px 12px; vertical-align: middle; }
  .empname { font-size: 15px; font-weight: bold; }
  .empcode { font-size: 10px; color: #444; text-transform: uppercase; letter-spacing: 0.5px; margin-top: 2px; }
  .emptag { font-size: 10px; color: #333; text-align: right; }
  .cards { width: 100%; border-collapse: collapse; margin-top: 12px; }
  .cards td { border: 1px solid #000; padding: 8px 6px; text-align: center; width: 25%; }
  .clabel { font-size: 8.5px; text-transform: uppercase; letter-spacing: 0.3px; }
  .cvalue { font-size: 14px; font-weight: bold; margin-top: 3px; }
  .csub { font-size: 7.5px; color: #555; margin-top: 2px; }
  .sec { font-size: 12px; font-weight: bold; text-transform: uppercase; letter-spacing: 0.5px; margin: 18px 0 5px; border-bottom: 1px solid #000; padding-bottom: 3px; }
  table.data { width: 100%; border-collapse: collapse; }
  table.data th { text-align: left; font-size: 9px; text-transform: uppercase; padding: 5px 6px; border: 1px solid #000; background: #ececec; }
  table.data td { font-size: 10px; padding: 4px 6px; border: 1px solid #999; }
  .num { text-align: right; white-space: nowrap; }
  .ft td { font-weight: bold; border-top: 2px solid #000; background: #f4f4f4; }
  .foot { margin-top: 20px; border-top: 1px solid #000; padding-top: 6px; font-size: 9px; color: #444; }
</style>
</head>
<body>
  <table class="hwrap"><tr>
    <td>
      <div class="company">{{companyName}}</div>
      <div class="cdetail">{{companyAddress}} | {{companyContact}} | GSTIN {{companyGstin}}</div>
    </td>
    <td class="rt">
      <div class="t">Employee Report</div>
      <div class="p">Period: {{fromDate}} to {{toDate}}</div>
      <div class="p">Generated: {{generatedOn}}</div>
    </td>
  </tr></table>

  <table class="empbar"><tr>
    <td>
      <div class="empname">{{employeeName}}</div>
      <div class="empcode">Code: {{employeeCode}}</div>
    </td>
    <td class="emptag">Piece-rate production summary<br/>for the selected period</td>
  </tr></table>

  <table class="cards"><tr>
    <td><div class="clabel">Total Pieces</div><div class="cvalue">{{totalQuantity}}</div><div class="csub">produced</div></td>
    <td><div class="clabel">Products</div><div class="cvalue">{{productsWorkedOn}}</div><div class="csub">worked on</div></td>
    <td><div class="clabel">Total Payment</div><div class="cvalue">{{totalEarnings}}</div><div class="csub">piece-rate</div></td>
    <td><div class="clabel">Avg Rate / Pc</div><div class="cvalue">{{avgRate}}</div><div class="csub">payment / pcs</div></td>
  </tr></table>

  <div class="sec">Daily Production</div>
  <table class="data">
    <thead><tr><th>Date</th><th>Product</th><th class="num">Pieces</th><th class="num">Rate</th><th class="num">Payment</th></tr></thead>
    <tbody>{{historyRows}}</tbody>
    <tfoot><tr class="ft"><td colspan="2">TOTAL</td><td class="num">{{totalQuantity}}</td><td></td><td class="num">{{totalEarnings}}</td></tr></tfoot>
  </table>

  <div class="sec">By Product</div>
  <table class="data">
    <thead><tr><th>Product</th><th class="num">Pieces</th><th class="num">Payment</th></tr></thead>
    <tbody>{{productRows}}</tbody>
    <tfoot><tr class="ft"><td>TOTAL</td><td class="num">{{totalQuantity}}</td><td class="num">{{totalEarnings}}</td></tr></tfoot>
  </table>

  <div class="foot">{{companyName}} &#8212; Employee Report &#160;&#160; Generated on {{generatedOn}}</div>
</body>
</html>', 'HTML template for the employee date-range report PDF', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('90de9576-25db-40a2-859e-cb8db67aac72', '00000000-0000-0000-0000-000000000001', 'LOW_STOCK_THRESHOLD', '50', 'Global minimum stock alert threshold', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('5675c047-9b10-40dc-8894-e32504ac4003', '00000000-0000-0000-0000-000000000001', 'MENU_ORDER', '["dashboard","production","inventory","products","investment","employees","sales","revenue","settings"]', 'Order of the left sidebar menu items (JSON array of keys, ascending)', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('24b8e159-fe1a-4f96-b55d-56ab25097de8', '00000000-0000-0000-0000-000000000001', 'PAYROLL_AUTO_SETTLE', 'false', NULL, now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('c2a2d3b0-2222-4b22-9b02-00000000e2b1', '00000000-0000-0000-0000-000000000001', 'PAYSLIP_TEMPLATE', '
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
', 'Pay-slip PDF template (salary / piece-work payment)', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('46abae0a-69c7-454f-83f9-f9eb0dce6398', '00000000-0000-0000-0000-000000000001', 'PRINT_ENABLED', 'false', 'Show Print buttons (invoices, reports, pay-slips): true / false', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('a0c516df-ffd4-4aca-b0a4-d57d1d5b8cfb', '00000000-0000-0000-0000-000000000001', 'PRODUCTION_PDF_TEMPLATE', '<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page { size: A4; margin: 32px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #000000; font-size: 12px; }
  table.hdr { width: 100%; }
  .hdr td { vertical-align: top; padding: 0; width: 50%; }
  .hdr .right { text-align: right; }
  .company { font-size: 18px; font-weight: bold; }
  .muted { color: #333333; font-size: 10px; margin-top: 2px; }
  .title { font-size: 15px; font-weight: bold; text-transform: uppercase; letter-spacing: 1px; }
  .meta { color: #333333; font-size: 11px; margin-top: 3px; }
  hr { border: none; border-top: 1.5px solid #000000; margin: 12px 0; }
  table.items { width: 100%; border-collapse: collapse; margin-top: 6px; }
  table.items th { border: 1px solid #000000; background: #eeeeee; padding: 7px 9px; text-align: left; font-size: 11px; }
  table.items td { border: 1px solid #000000; padding: 7px 9px; font-size: 12px; }
  .right { text-align: right; white-space: nowrap; }
  .tot td { font-weight: bold; background: #f6f6f6; }
  .foot { margin-top: 16px; color: #555555; font-size: 9px; text-align: center; }
</style>
</head>
<body>
  <table class="hdr">
    <tr>
      <td>
        <div class="company">{{companyName}}</div>
        <div class="muted">{{companyAddress}}</div>
        <div class="muted">{{companyContact}} | GSTIN {{companyGstin}}</div>
      </td>
      <td class="right">
        <div class="title">Work Report</div>
        <div class="meta">Ref: {{refId}}</div>
        <div class="meta">Work Date: {{workDate}}</div>
        <div class="meta">Employee: {{employeeName}}</div>
      </td>
    </tr>
  </table>
  <hr/>
  <table class="items">
    <tr><th>Work Name</th><th class="right">Quantity</th><th class="right">Work Rate</th><th class="right">Amount</th></tr>
    {{itemRows}}
    <tr class="tot"><td>Total Work Item: {{totalWorkItems}}</td><td class="right">{{totalQuantity}}</td><td></td><td class="right">{{totalAmount}}</td></tr>
  </table>
  <div class="foot">Generated on {{generatedOn}}</div>
</body>
</html>', 'HTML template for production PDF export (placeholders filled at runtime)', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('b1f1c2a0-1111-4a11-9a01-00000000d1a1', '00000000-0000-0000-0000-000000000001', 'PRODUCTION_REPORT_TEMPLATE', '<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page { size: A4; margin: 28px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #000000; font-size: 11px; }
  table.hdr { width: 100%; }
  .hdr td { vertical-align: top; padding: 0; width: 50%; }
  .hdr .right { text-align: right; }
  .company { font-size: 18px; font-weight: bold; }
  .muted { color: #333333; font-size: 10px; margin-top: 2px; }
  .title { font-size: 15px; font-weight: bold; text-transform: uppercase; letter-spacing: 1px; }
  .period { color: #333333; font-size: 11px; margin-top: 2px; }
  hr { border: none; border-top: 1.5px solid #000000; margin: 10px 0; }
  table.cards { width: 100%; border-collapse: collapse; margin-top: 6px; }
  table.cards td { border: 1px solid #000000; background: #f6f6f6; padding: 10px; width: 33%; }
  .clabel { color: #333333; font-size: 10px; }
  .cvalue { font-size: 15px; font-weight: bold; }
  table.items { width: 100%; border-collapse: collapse; margin-top: 14px; }
  table.items th { border: 1px solid #000000; background: #eeeeee; padding: 6px 8px; text-align: left; font-size: 10px; }
  table.items td { border: 1px solid #000000; padding: 6px 8px; }
  .num { text-align: right; white-space: nowrap; }
  .tot td { font-weight: bold; background: #f6f6f6; }
  .foot { margin-top: 16px; color: #555555; font-size: 9px; text-align: center; }
</style>
</head>
<body>
  <table class="hdr">
    <tr>
      <td>
        <div class="company">{{companyName}}</div>
        <div class="muted">{{companyAddress}}</div>
        <div class="muted">{{companyContact}} | GSTIN {{companyGstin}}</div>
      </td>
      <td class="right">
        <div class="title">Work Report</div>
        <div class="period">Period: {{fromDate}} to {{toDate}}</div>
      </td>
    </tr>
  </table>
  <hr/>
  <table class="cards">
    <tr>
      <td><div class="clabel">Entries</div><div class="cvalue">{{entries}}</div></td>
      <td><div class="clabel">Total Piece</div><div class="cvalue">{{totalPiece}}</div></td>
      <td><div class="clabel">Total Payment</div><div class="cvalue">{{totalPayment}}</div></td>
    </tr>
  </table>
  <table class="items">
    <tr><th>Work Date</th><th>Employee Name</th><th>Work Name</th><th class="num">Quantity</th><th class="num">Work Rate</th><th class="num">Amount</th></tr>
    {{workRows}}
    <tr class="tot"><td colspan="3">Total</td><td class="num">{{totalQuantity}}</td><td></td><td class="num">{{totalAmount}}</td></tr>
  </table>
  <div class="foot">Generated on {{generatedOn}}</div>
</body>
</html>', 'HTML template for the production date-range report PDF', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('794c4226-76b1-4fa3-a7c5-0bebb1b50aaf', '00000000-0000-0000-0000-000000000001', 'PURCHASE_INVOICE_TEMPLATE', '<html>
<head>
<style>
  @page { size: A4; margin: 34px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #1a1a1a; font-size: 12px; }
  .head { width: 100%; border-collapse: collapse; }
  .head td { vertical-align: top; }
  .cname { font-size: 20px; font-weight: bold; letter-spacing: 0.5px; }
  .csub { font-size: 9px; color: #555555; letter-spacing: 1px; margin-top: 2px; }
  .caddr { font-size: 10px; color: #444444; margin-top: 6px; line-height: 1.5; }
  .doc-title { font-size: 22px; font-weight: bold; text-align: right; letter-spacing: 3px; }
  .doc-sub { font-size: 10px; color: #555555; text-align: right; letter-spacing: 3px; margin-top: 2px; }
  .rule { border-bottom: 2px solid #000000; margin: 8px 0 14px 0; }
  .parties { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
  .parties td { vertical-align: top; width: 50%; padding-right: 18px; }
  .eyebrow { font-size: 9px; font-weight: bold; color: #888888; letter-spacing: 1px; margin-bottom: 5px; }
  .pname { font-size: 13px; font-weight: bold; }
  .pline { font-size: 11px; color: #444444; margin-top: 2px; }
  .meta-t { width: 100%; border-collapse: collapse; }
  .meta-t td { padding: 3px 0; font-size: 11px; }
  .meta-k { color: #666666; }
  .meta-v { font-weight: bold; text-align: right; }
  table.items { width: 100%; border-collapse: collapse; margin: 4px 0 12px 0; }
  table.items th { text-align: left; padding: 8px; font-size: 10px; letter-spacing: 0.5px; border-top: 1.5px solid #000000; border-bottom: 1.5px solid #000000; background-color: #f2f2f0; }
  table.items td { padding: 9px 8px; border-bottom: 1px solid #dddddd; font-size: 11px; vertical-align: top; }
  .right { text-align: right; white-space: nowrap; }
  .itype { font-size: 9px; color: #666666; margin-top: 2px; }
  .totals { width: 46%; margin-left: 54%; border-collapse: collapse; margin-bottom: 4px; }
  .totals td { padding: 5px 8px; font-size: 11px; }
  .totals .k { color: #555555; }
  .totals .grand td { border-top: 2px solid #000000; font-size: 15px; font-weight: bold; padding-top: 9px; }
  .pay-h { font-size: 10px; font-weight: bold; letter-spacing: 1px; border-bottom: 1.5px solid #000000; padding-bottom: 5px; margin-top: 14px; }
  .pay-sum { width: 100%; border-collapse: collapse; margin: 10px 0; }
  .pay-sum td { font-size: 11px; padding: 2px 0; }
  table.ph { width: 100%; border-collapse: collapse; }
  table.ph th { text-align: left; font-size: 9px; color: #666666; letter-spacing: 0.5px; padding: 5px 8px; border-bottom: 1px solid #cccccc; }
  table.ph td { font-size: 11px; padding: 7px 8px; border-bottom: 1px solid #eeeeee; }
  .end { width: 100%; border-collapse: collapse; margin-top: 28px; }
  .end td { vertical-align: bottom; }
  .terms { font-size: 10px; color: #555555; width: 62%; line-height: 1.6; }
  .sign { font-size: 10px; color: #555555; text-align: center; }
  .sline { border-top: 1px solid #000000; padding-top: 4px; margin-top: 40px; }
  .foot { margin-top: 18px; border-top: 1px solid #dddddd; padding-top: 8px; text-align: center; color: #999999; font-size: 9px; }
</style>
</head>
<body>
  <table class="head">
    <tr>
      <td>
        <div class="cname">{{companyName}}</div>
        <div class="csub">MANUFACTURING AND TRADING</div>
        <div class="caddr">{{companyAddress}}</div>
        <div class="caddr">{{companyContact}} &#160;&#183;&#160; GSTIN {{companyGstin}}</div>
      </td>
      <td>
        <div class="doc-title">INVOICE</div>
        <div class="doc-sub">PURCHASE</div>
      </td>
    </tr>
  </table>
  <div class="rule"></div>

  <table class="parties">
    <tr>
      <td>
        <div class="eyebrow">VENDOR</div>
        <div class="pname">{{vendorName}}</div>
        <div class="pline">{{vendorAddress}}</div>
        <div class="pline">{{vendorContact}}</div>
        <div class="pline">GSTIN {{vendorGstin}}</div>
      </td>
      <td>
        <table class="meta-t">
          <tr><td class="meta-k">Invoice No.</td><td class="meta-v">{{invoiceNumber}}</td></tr>
          <tr><td class="meta-k">Invoice Date</td><td class="meta-v">{{invoiceDate}}</td></tr>
          <tr><td class="meta-k">Type</td><td class="meta-v">{{invoiceType}}</td></tr>
          <tr><td class="meta-k">Status</td><td class="meta-v">{{paymentStatus}}</td></tr>
        </table>
      </td>
    </tr>
  </table>

  <table class="items">
    <thead>
      <tr>
        <th style="width: 26px;">#</th>
        <th>DESCRIPTION</th>
        <th class="right" style="width: 70px;">QTY</th>
        <th style="width: 58px;">UNIT</th>
        <th class="right" style="width: 82px;">RATE</th>
        <th class="right" style="width: 96px;">AMOUNT</th>
      </tr>
    </thead>
    <tbody>
      {{itemRows}}
    </tbody>
  </table>

  <table class="totals">
    <tr><td class="k">Subtotal</td><td class="right">{{subtotal}}</td></tr>
    <tr><td class="k">GST</td><td class="right">{{gst}}</td></tr>
    <tr><td class="k">Discount</td><td class="right">- {{discount}}</td></tr>
    
    <tr class="grand"><td>Grand Total</td><td class="right">{{grandTotal}}</td></tr>
  </table>

  <div class="pay-h">PAYMENT DETAILS</div>
  <table class="pay-sum">
    <tr>
      <td>Amount Paid: <b>{{amountPaid}}</b></td>
      <td>Balance Due: <b>{{amountDue}}</b></td>
      <td>Status: <b>{{paymentStatus}}</b></td>
    </tr>
  </table>
  <table class="ph">
    <thead>
      <tr><th style="width: 130px;">DATE</th><th>MODE</th><th class="right" style="width: 110px;">AMOUNT</th></tr>
    </thead>
    <tbody>
      {{paymentRows}}
    </tbody>
  </table>

  <table class="end">
    <tr>
      <td class="terms"><b>Terms.</b> Goods received in full and inspected. Balance payable within 30 days of the invoice date. Please quote the invoice number on all payments.</td>
      <td class="sign"><div>For {{companyName}}</div><div class="sline">Authorised Signatory</div></td>
    </tr>
  </table>

  <div class="foot">Generated on {{generatedOn}}</div>
</body>
</html>', 'HTML template for the purchase (vendor) invoice PDF (placeholders filled at runtime)', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('88ee4f90-c078-48ba-9d4d-793d01a5d5dd', '00000000-0000-0000-0000-000000000001', 'QR_LABEL_CONFIG', '{"showName":true,"showPrice":true,"showSerial":true,"nameVertical":false}', 'QR label print settings: show/hide fields and product-name orientation', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('d1767e99-9202-4d62-b8a7-e64da7abda9f', '00000000-0000-0000-0000-000000000001', 'ROLE_PERMISSIONS', '{"STAFF": {"sales": {"view": true, "write": true, "delete": false}, "revenue": {"view": false, "write": false, "delete": false}, "products": {"view": true, "write": false, "delete": false}, "settings": {"view": false, "write": false, "delete": false}, "dashboard": {"view": true, "write": false, "delete": false}, "employees": {"view": false, "write": false, "delete": false}, "inventory": {"view": true, "write": false, "delete": false}, "investment": {"view": true, "write": false, "delete": false}, "production": {"view": false, "write": false, "delete": false}}}', 'Per-role, per-module permissions (view/write/delete) for STAFF; ADMIN is always full access', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('44f481d4-be41-49e5-ab06-e4f6f5a9e6e9', '00000000-0000-0000-0000-000000000001', 'SALES_GST_ENABLED', 'false', 'When true, GST (DEFAULT_GST_PERCENT) is added to every new sale; when false, sales have no GST', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('7407a6aa-a7d0-4b6d-ab7b-b4850e053f78', '00000000-0000-0000-0000-000000000001', 'SALES_INVOICE_TEMPLATE', '<html>
<head>
<style>
  @page { size: A4; margin: 32px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #000; font-size: 12px; }
  .head { width: 100%; border-collapse: collapse; margin-bottom: 8px; }
  .head td { vertical-align: top; }
  .brand { font-size: 24px; font-weight: bold; color: #000; letter-spacing: 1px; }
  .inv-title { font-size: 26px; font-weight: bold; color: #000; text-align: right; }
  .inv-no { font-size: 12px; color: #555; text-align: right; }
  .divider { border-bottom: 3px solid #000; margin: 6px 0 16px 0; }
  .meta { width: 100%; border-collapse: collapse; margin-bottom: 18px; }
  .meta td { vertical-align: top; padding: 0 6px; width: 50%; }
  .label { font-size: 10px; font-weight: bold; color: #555; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 3px; }
  .value { font-size: 13px; font-weight: bold; color: #000; }
  .muted { color: #555; font-size: 11px; margin-top: 2px; }
  .badge { display: inline-block; padding: 3px 10px; border: 1px solid #000; border-radius: 10px; font-size: 10px; font-weight: bold; color: #000; }
  .badge-paid { background-color: #ffffff; }
  .badge-other { background-color: #dcdcdc; }
  table.items { width: 100%; border-collapse: collapse; margin-bottom: 14px; table-layout: fixed; }
  table.items th { background-color: #ececec; color: #000; text-align: left; padding: 8px 10px; font-size: 11px; border: 1px solid #000; }
  table.items td { padding: 8px 10px; border-bottom: 1px solid #999; word-wrap: break-word; }
  .right { text-align: right; white-space: nowrap; }
  .totals { width: 46%; margin-left: 54%; border-collapse: collapse; }
  .totals td { padding: 5px 10px; }
  .totals .grand td { border-top: 2px solid #000; font-size: 15px; font-weight: bold; color: #000; }
  .footer { margin-top: 30px; border-top: 1px solid #999; padding-top: 10px; color: #555; font-size: 10px; text-align: center; }
</style>
</head>
<body>
  <table class="head">
    <tr>
      <td>
        <div class="brand">{{companyName}}</div>
        <div class="muted">{{companyAddress}}</div>
        <div class="muted">{{companyContact}} &#160;&#183;&#160; GSTIN {{companyGstin}}</div>
      </td>
      <td>
        <div class="inv-title">INVOICE</div>
        <div class="inv-no">{{invoiceNumber}}</div>
      </td>
    </tr>
  </table>
  <div class="divider"></div>

  <table class="meta">
    <tr>
      <td>
        <div class="label">Billed To</div>
        <div class="value">{{customerName}}</div>
        <div class="muted">{{customerMobile}}</div>
        <div class="muted">{{customerEmail}}</div>
      </td>
      <td>
        <div class="label">Invoice Date</div>
        <div class="value">{{date}}</div>
        <div class="label" style="margin-top:8px;">Payment</div>
        <div class="value">{{paymentMode}} <span class="badge {{statusClass}}">{{paymentStatus}}</span></div>
        <div class="muted">{{paymentProvider}}</div>
      </td>
    </tr>
  </table>

  <table class="items">
    <thead>
      <tr>
        <th>Product</th>
        <th class="right" style="width: 62px;">Qty</th>
        <th class="right" style="width: 92px;">Price</th>
        <th class="right" style="width: 92px;">Discount</th>
        <th class="right" style="width: 104px;">Amount</th>
      </tr>
    </thead>
    <tbody>
      {{itemRows}}
    </tbody>
  </table>

  <table class="totals">
    <tr><td>Subtotal</td><td class="right">{{cur}} {{subtotal}}</td></tr>
    <tr><td>Discount</td><td class="right">- {{cur}} {{discount}}</td></tr>
    <tr><td>Tax</td><td class="right">{{cur}} {{tax}}</td></tr>
    <tr class="grand"><td>Grand Total</td><td class="right">{{cur}} {{grandTotal}}</td></tr>
    {{paymentRows}}
  </table>

  <div class="footer">Thank you for your business. Generated on {{generatedOn}}.</div>
</body>
</html>', 'HTML template for sales invoice PDF (placeholders filled at runtime)', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('b2f2c2a0-2222-4a22-9a02-00000000d2a2', '00000000-0000-0000-0000-000000000001', 'SALES_REPORT_TEMPLATE', '<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page { size: A4; margin: 24px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #000; font-size: 11px; }
  .hwrap { width: 100%; border-collapse: collapse; border-bottom: 2px solid #000; }
  .hwrap td { vertical-align: top; padding: 0 0 9px; }
  .company { font-size: 18px; font-weight: bold; letter-spacing: 0.3px; }
  .cdetail { font-size: 9.5px; color: #333; margin-top: 3px; line-height: 1.5; }
  .rt { text-align: right; white-space: nowrap; }
  .rt .t { font-size: 15px; font-weight: bold; text-transform: uppercase; letter-spacing: 1px; }
  .rt .p { font-size: 10px; margin-top: 3px; }
  .cards { width: 100%; border-collapse: collapse; margin-top: 14px; }
  .cards td { border: 1px solid #000; padding: 8px 4px; text-align: center; width: 14.28%; }
  .clabel { font-size: 8px; text-transform: uppercase; letter-spacing: 0.3px; }
  .cvalue { font-size: 12px; font-weight: bold; margin-top: 3px; }
  .csub { font-size: 7px; color: #555; margin-top: 2px; }
  .sec { font-size: 12px; font-weight: bold; text-transform: uppercase; letter-spacing: 0.5px; margin: 18px 0 5px; border-bottom: 1px solid #000; padding-bottom: 3px; }
  table.data { width: 100%; border-collapse: collapse; }
  table.data th { text-align: left; font-size: 9px; text-transform: uppercase; padding: 5px 6px; border: 1px solid #000; background: #ececec; }
  table.data td { font-size: 10px; padding: 4px 6px; border: 1px solid #999; }
  .num { text-align: right; white-space: nowrap; }
  .ctr { text-align: center; }
  .ft td { font-weight: bold; border-top: 2px solid #000; background: #f4f4f4; }
  .st { font-size: 9px; font-weight: bold; text-transform: uppercase; padding: 1px 6px; }
  .st.partial { background: #dcdcdc; }
  .st.unpaid { background: #bdbdbd; }
  .legend { font-size: 9px; color: #444; margin-top: 5px; }
  .foot { margin-top: 20px; border-top: 1px solid #000; padding-top: 6px; font-size: 9px; color: #444; }
</style>
</head>
<body>
  <table class="hwrap"><tr>
    <td>
      <div class="company">{{companyName}}</div>
      <div class="cdetail">{{companyAddress}} | {{companyContact}} | GSTIN {{companyGstin}}</div>
    </td>
    <td class="rt">
      <div class="t">Sales Report</div>
      <div class="p">Period: {{fromDate}} to {{toDate}}</div>
      <div class="p">Generated: {{generatedOn}}</div>
    </td>
  </tr></table>

  <table class="cards"><tr>
    <td><div class="clabel">Total Sales</div><div class="cvalue">{{totalSales}}</div><div class="csub">invoices</div></td>
    <td><div class="clabel">MRP Total</div><div class="cvalue">{{mrpTotal}}</div><div class="csub">actual SP</div></td>
    <td><div class="clabel">Sale</div><div class="cvalue">{{saleAmount}}</div><div class="csub">selling price</div></td>
    <td><div class="clabel">Discount</div><div class="cvalue">{{totalDiscount}}</div><div class="csub">given</div></td>
    <td><div class="clabel">Due</div><div class="cvalue">{{totalDue}}</div><div class="csub">outstanding</div></td>
    <td><div class="clabel">GST</div><div class="cvalue">{{totalTax}}</div><div class="csub">tax</div></td>
    <td><div class="clabel">Net Profit</div><div class="cvalue">{{totalProfit}}</div><div class="csub">after disc.</div></td>
  </tr></table>

  <div class="sec">Invoice Details</div>
  <table class="data">
    <thead><tr>
      <th>Inv No</th><th>Date</th><th>Customer</th><th>Mobile</th>
      <th class="num">Sale Amount (incl GST)</th><th class="num">Received</th><th class="num">Due</th><th class="ctr">Status</th>
    </tr></thead>
    <tbody>{{invoiceRows}}</tbody>
    <tfoot><tr class="ft">
      <td colspan="4">TOTAL ({{totalSales}} invoices)</td>
      <td class="num">{{invTotalSale}}</td><td class="num">{{invTotalReceived}}</td><td class="num">{{invTotalDue}}</td><td></td>
    </tr></tfoot>
  </table>
  <div class="legend">Paid = fully received &#160;|&#160; Partial = part received (shaded) &#160;|&#160; Unpaid = udhaar / due (darker shade)</div>

  <div class="sec">By Product</div>
  <table class="data">
    <thead><tr><th>Product</th><th class="num">Qty Sold</th><th class="num">Sale Amount</th><th class="num">Profit</th></tr></thead>
    <tbody>{{productRows}}</tbody>
  </table>

  <div class="foot">{{companyName}} &#8212; Sales Report &#160;&#160; Generated on {{generatedOn}}</div>
</body>
</html>', 'HTML template for the sales date-range report PDF', now(), now());
INSERT INTO system_setting (id, portfolio_id, setting_key, setting_value, description, created_at, updated_at) VALUES ('2fa87584-6e37-4d50-8b76-369cddfeca16', '00000000-0000-0000-0000-000000000001', 'STAFF_DASHBOARD_BOARDS', '["sales","inventory"]', 'Dashboard boards visible to STAFF (JSON array of board keys); ADMIN sees all', now(), now());
