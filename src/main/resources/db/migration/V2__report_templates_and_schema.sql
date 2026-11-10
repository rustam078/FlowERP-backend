-- Date-range report HTML templates (company header filled from COMPANY_* settings at render time).

INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES
('b1f1c2a0-1111-4a11-9a01-00000000d1a1', 'PRODUCTION_REPORT_TEMPLATE', '<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page { size: A4; margin: 28px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #0F172A; font-size: 11px; }
  .company { font-size: 18px; font-weight: bold; }
  .muted { color: #64748B; font-size: 10px; }
  .title { font-size: 15px; font-weight: bold; margin-top: 12px; }
  .period { color: #475569; font-size: 11px; margin-top: 2px; }
  hr { border: none; border-top: 1px solid #E2E8F0; margin: 10px 0; }
  table { width: 100%; border-collapse: collapse; margin-top: 6px; }
  th { background: #F1F5F9; text-align: left; padding: 6px 8px; font-size: 10px; color: #475569; border-bottom: 1px solid #E2E8F0; }
  td { padding: 6px 8px; border-bottom: 1px solid #EEF2F6; }
  .num { text-align: right; }
  .cards td { border: 1px solid #E2E8F0; background: #F8FAFC; padding: 10px; width: 33%; }
  .clabel { color: #64748B; font-size: 10px; }
  .cvalue { font-size: 16px; font-weight: bold; }
  .sec { font-size: 12px; font-weight: bold; margin-top: 16px; }
  .foot { margin-top: 18px; color: #94A3B8; font-size: 9px; text-align: center; }
</style>
</head>
<body>
  <div class="company">{{companyName}}</div>
  <div class="muted">{{companyAddress}} | {{companyContact}} | GSTIN {{companyGstin}}</div>
  <div class="title">Production report</div>
  <div class="period">Period: {{fromDate}} to {{toDate}}</div>
  <hr/>
  <table class="cards">
    <tr>
      <td><div class="clabel">Entries</div><div class="cvalue">{{totalEntries}}</div></td>
      <td><div class="clabel">Total pieces</div><div class="cvalue">{{totalQuantity}}</div></td>
      <td><div class="clabel">Total payment</div><div class="cvalue">{{totalAmount}}</div></td>
    </tr>
  </table>
  <div class="sec">By product</div>
  <table>
    <thead><tr><th>Product</th><th class="num">Pieces</th><th class="num">Payment</th></tr></thead>
    <tbody>{{productRows}}</tbody>
  </table>
  <div class="sec">By employee</div>
  <table>
    <thead><tr><th>Employee</th><th class="num">Pieces</th><th class="num">Payment</th></tr></thead>
    <tbody>{{employeeRows}}</tbody>
  </table>
  <div class="foot">Generated on {{generatedOn}}</div>
</body>
</html>', 'HTML template for the production date-range report PDF', now(), now());

INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES
('b2f2c2a0-2222-4a22-9a02-00000000d2a2', 'SALES_REPORT_TEMPLATE', '<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page { size: A4; margin: 24px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #000; font-size: 11px; }
  .hwrap { width: 100%; border-collapse: collapse; border-bottom: 2px solid #000; }
  .hwrap td { vertical-align: top; padding: 0 0 9px; }
  .company { font-size: 18px; font-weight: bold; letter-spacing: 0.3px; }
  .cdetail { font-size: 9.5px; color: #333; margin-top: 3px; line-height: 1.5; }
  .rt { text-align: right; }
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
  .num { text-align: right; }
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

INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES
('b3f3c2a0-3333-4a33-9a03-00000000d3a3', 'EMPLOYEE_REPORT_TEMPLATE', '<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page { size: A4; margin: 24px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #000; font-size: 11px; }
  .hwrap { width: 100%; border-collapse: collapse; border-bottom: 2px solid #000; }
  .hwrap td { vertical-align: top; padding: 0 0 9px; }
  .company { font-size: 18px; font-weight: bold; letter-spacing: 0.3px; }
  .cdetail { font-size: 9.5px; color: #333; margin-top: 3px; line-height: 1.5; }
  .rt { text-align: right; }
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
  .num { text-align: right; }
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

-- Consolidated schema evolution (folds the former incremental migrations into one clean step).
-- Safe for a fresh database: runs after the baseline schema/data; no-op data back-fills were dropped,
-- and the short-lived opening_advance column (added then removed) is simply never created.

-- Investment: per-item selling price + payment grouping
ALTER TABLE investment_item ADD COLUMN IF NOT EXISTS selling_price numeric(14, 2);
ALTER TABLE investment_payment ADD COLUMN IF NOT EXISTS payment_group_id uuid;

-- Sales credit: how much of a sale is paid / was received at checkout
ALTER TABLE sales_order ADD COLUMN IF NOT EXISTS amount_paid numeric(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE sales_order ADD COLUMN IF NOT EXISTS amount_received numeric(19, 2) NOT NULL DEFAULT 0;

-- Payments received against sales (one UI payment can span several sales via payment_group_id)
CREATE TABLE IF NOT EXISTS customer_payment (
    id uuid NOT NULL,
    sales_order_id uuid NOT NULL,
    payment_date date NOT NULL,
    mode character varying(20) NOT NULL,
    amount numeric(18, 2) NOT NULL,
    payment_group_id uuid,
    reference_invoice_no varchar(50),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone,
    CONSTRAINT customer_payment_pkey PRIMARY KEY (id),
    CONSTRAINT fk_customer_payment_sale FOREIGN KEY (sales_order_id)
        REFERENCES sales_order (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_customer_payment_sale ON customer_payment (sales_order_id);

-- Production becomes employee piece-work: free-text work, optional product link, no remarks
ALTER TABLE production_entry DROP COLUMN IF EXISTS remarks;
ALTER TABLE production_entry_detail ADD COLUMN IF NOT EXISTS work VARCHAR(255);
ALTER TABLE production_entry_detail ALTER COLUMN product_id DROP NOT NULL;

-- Employee pay model: piece-rate vs monthly salary + denormalized running advance balance
ALTER TABLE employee ADD COLUMN IF NOT EXISTS payment_type VARCHAR(20) NOT NULL DEFAULT 'PIECE_RATE';
ALTER TABLE employee ADD COLUMN IF NOT EXISTS monthly_salary NUMERIC(12,2);
ALTER TABLE employee ADD COLUMN IF NOT EXISTS advance_balance NUMERIC(18,2) NOT NULL DEFAULT 0;

-- Exception-based attendance (only absent / half-day are stored)
CREATE TABLE IF NOT EXISTS employee_attendance (
    id          UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    date        DATE NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    CONSTRAINT uq_attendance_emp_date UNIQUE (employee_id, date)
);
CREATE INDEX IF NOT EXISTS idx_attendance_emp_date ON employee_attendance (employee_id, date);

-- Payroll ledger: advances, expenses and settlements (period links a settlement to its pay-slip month)
CREATE TABLE IF NOT EXISTS employee_ledger (
    id          UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    entry_date  DATE NOT NULL,
    type        VARCHAR(20) NOT NULL,
    amount      NUMERIC(18,2) NOT NULL,
    mode        VARCHAR(20),
    note        VARCHAR(200),
    period      DATE,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ledger_emp_date ON employee_ledger (employee_id, entry_date);
CREATE INDEX IF NOT EXISTS idx_ledger_emp_period ON employee_ledger (employee_id, period);
