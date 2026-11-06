

INSERT INTO public.app_user (id, username, password, full_name, role, is_active, created_at, updated_at) VALUES ('aee223cc-d162-4e48-98cc-6b47350ba02f', 'admin', 'admin123', 'System Administrator', 'ADMIN', true, '2026-09-28 09:32:26.399994', '2026-09-28 09:32:26.399994');

INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('90de9576-25db-40a2-859e-cb8db67aac72', 'LOW_STOCK_THRESHOLD', '50', 'Global minimum stock alert threshold', '2026-09-28 09:32:26.747143', '2026-09-28 09:32:26.747143');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('88ee4f90-c078-48ba-9d4d-793d01a5d5dd', 'QR_LABEL_CONFIG', '{"showName":true,"showPrice":true,"showSerial":true,"nameVertical":false}', 'QR label print settings: show/hide fields and product-name orientation', '2026-09-28 09:32:26.846862', '2026-09-28 09:32:26.846862');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('bc1b9621-13e6-47de-ba08-08dc613a272f', 'COMPANY_NAME', 'SKR Garment', 'Company name shown on invoices and headers', '2026-09-28 09:32:26.870405', '2026-09-28 09:32:26.870405');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('6d03d7d2-f735-4a96-bbbd-1718fb217221', 'COMPANY_ADDRESS', '14 Loom Street, Industrial Area, Patna, Bihar 800001', 'Company address shown on invoices', '2026-09-28 09:32:26.870405', '2026-09-28 09:32:26.870405');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('cf220e2a-27b6-4e8e-8033-5f846ae58b6b', 'COMPANY_CONTACT', '+91 90000 12345', 'Company phone / email shown on invoices', '2026-09-28 09:32:26.870405', '2026-09-28 09:32:26.870405');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('5e11a702-f8d0-4328-b137-85a191279cb2', 'COMPANY_GSTIN', '10ABCDE1234F1Z5', 'Company GST identification number', '2026-09-28 09:32:26.870405', '2026-09-28 09:32:26.870405');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('a5ac9cd5-50cf-4657-9828-7c5b832fc968', 'DEFAULT_GST_PERCENT', '5', 'Default GST percent applied to new invoices (snapshotted per invoice)', '2026-09-28 09:32:26.870405', '2026-09-28 09:32:26.870405');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('aeef3a15-08de-466b-bf45-79a5d23c81f8', 'CURRENCY_SYMBOL', '₹', 'Currency symbol used across the app', '2026-09-28 09:32:26.870405', '2026-09-28 09:32:26.870405');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('794c4226-76b1-4fa3-a7c5-0bebb1b50aaf', 'PURCHASE_INVOICE_TEMPLATE', '<html>
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
  .right { text-align: right; }
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
    <tr><td class="k">Other Charges</td><td class="right">{{otherCharge}}</td></tr>
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
</html>', 'HTML template for the purchase (vendor) invoice PDF (placeholders filled at runtime)', '2026-09-28 09:32:26.882718', '2026-09-28 09:32:26.882718');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('5675c047-9b10-40dc-8894-e32504ac4003', 'MENU_ORDER', '["dashboard","production","inventory","products","investment","employees","sales","settings"]', 'Order of the left sidebar menu items (JSON array of keys, ascending)', '2026-09-28 09:32:26.89465', '2026-09-28 09:32:26.89465');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('44f481d4-be41-49e5-ab06-e4f6f5a9e6e9', 'SALES_GST_ENABLED', 'false', 'When true, GST (DEFAULT_GST_PERCENT) is added to every new sale; when false, sales have no GST', '2026-09-28 09:32:26.909267', '2026-09-28 09:32:26.909267');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('7407a6aa-a7d0-4b6d-ab7b-b4850e053f78', 'SALES_INVOICE_TEMPLATE', '<html>
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
  .right { text-align: right; }
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
</html>', 'HTML template for sales invoice PDF (placeholders filled at runtime)', '2026-09-28 09:32:26.776259', '2026-09-28 09:32:26.776259');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('a0c516df-ffd4-4aca-b0a4-d57d1d5b8cfb', 'PRODUCTION_PDF_TEMPLATE', '<html>
<head>
<style>
  @page { size: A4; margin: 26px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #1E293B; font-size: 12px; }
  .header { text-align: center; border-bottom: 3px solid #2563EB; padding-bottom: 10px; margin-bottom: 16px; }
  .brand { font-size: 22px; font-weight: bold; color: #2563EB; letter-spacing: 1px; }
  .subtitle { font-size: 11px; color: #64748B; margin-top: 2px; }
  .report-title { font-size: 15px; font-weight: bold; margin-top: 8px; color: #0F172A; }
  .ref { color: #64748B; font-size: 11px; margin-bottom: 8px; }
  .info-table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
  .info-table td { padding: 5px 6px; vertical-align: top; }
  .info-label { color: #64748B; font-weight: bold; width: 130px; }
  .items { width: 100%; border-collapse: collapse; margin-bottom: 14px; table-layout: fixed; }
  .items th { background-color: #F1F5F9; color: #334155; text-align: left; padding: 7px 8px; border: 1px solid #E2E8F0; font-size: 11px; }
  .items td { padding: 7px 8px; border: 1px solid #E2E8F0; word-wrap: break-word; }
  .right { text-align: right; }
  .summary { width: 45%; margin-left: 55%; border-collapse: collapse; margin-top: 4px; }
  .summary td { padding: 5px 8px; border-bottom: 1px solid #E2E8F0; }
  .summary .tot { font-weight: bold; color: #047857; font-size: 13px; }
  .footer { margin-top: 26px; text-align: right; color: #94A3B8; font-size: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
</style>
</head>
<body>
  <div class="header">
    <div class="brand">{{companyName}}</div>
    <div class="subtitle">{{companyAddress}}</div>
    <div class="subtitle">{{companyContact}} &#160;&#183;&#160; GSTIN {{companyGstin}}</div>
    <div class="report-title">Production Report</div>
  </div>
  <div class="ref">Ref ID: {{refId}}</div>
  <table class="info-table">
    <tr><td class="info-label">Production Date</td><td>{{productionDate}}</td></tr>
    <tr><td class="info-label">Employee</td><td>{{employeeName}}</td></tr>
    <tr><td class="info-label">Remarks</td><td>{{remarks}}</td></tr>
  </table>
  <table class="items">
    <thead>
      <tr>
        <th>Product</th>
        <th style="width: 110px;">Piece Code</th>
        <th class="right" style="width: 80px;">Quantity</th>
        <th class="right" style="width: 90px;">Rate</th>
        <th class="right" style="width: 100px;">Amount</th>
      </tr>
    </thead>
    <tbody>
      {{itemRows}}
    </tbody>
  </table>
  <table class="summary">
    <tr><td>Total Products</td><td class="right">{{productCount}}</td></tr>
    <tr><td>Total Quantity</td><td class="right">{{totalQuantity}} Pcs</td></tr>
    <tr><td class="tot">Total Amount</td><td class="right tot">Rs. {{totalAmount}}</td></tr>
  </table>
  <div class="footer">Generated on {{generatedOn}}</div>
</body>
</html>', 'HTML template for production PDF export (placeholders filled at runtime)', '2026-09-28 09:32:26.765098', '2026-09-28 09:32:26.765098');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('d1767e99-9202-4d62-b8a7-e64da7abda9f', 'ROLE_PERMISSIONS', '{"STAFF":{"dashboard":{"view":true,"write":false,"delete":false},"sales":{"view":true,"write":true,"delete":false},"inventory":{"view":true,"write":false,"delete":false},"products":{"view":true,"write":false,"delete":false},"investment":{"view":true,"write":false,"delete":false},"production":{"view":false,"write":false,"delete":false},"employees":{"view":false,"write":false,"delete":false},"settings":{"view":false,"write":false,"delete":false}}}', 'Per-role, per-module permissions (view/write/delete) for STAFF; ADMIN is always full access', '2026-09-28 09:32:26.931597', '2026-09-28 09:32:26.931597');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('2fa87584-6e37-4d50-8b76-369cddfeca16', 'STAFF_DASHBOARD_BOARDS', '["sales","inventory"]', 'Dashboard boards visible to STAFF (JSON array of board keys); ADMIN sees all', '2026-09-28 09:32:26.940362', '2026-09-28 09:32:26.940362');


