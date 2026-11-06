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
