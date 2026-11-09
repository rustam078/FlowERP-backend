-- Piece-work reports rebranded to "Work Report" (black & white): the per-entry doc and the
-- date-range report. Work Name = free-text work; piece code / remarks / by-product removed.

-- Per-entry work doc (row-wise download from the table's Action column)
UPDATE system_setting SET setting_value = '<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page { size: A4; margin: 32px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #000000; font-size: 12px; }
  .hdr td { vertical-align: top; padding: 0; }
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
</html>', updated_at = now()
WHERE setting_key = 'PRODUCTION_PDF_TEMPLATE';

-- Date-range work report (Download Work Report dialog)
UPDATE system_setting SET setting_value = '<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<style>
  @page { size: A4; margin: 28px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #000000; font-size: 11px; }
  .hdr td { vertical-align: top; padding: 0; }
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
        <div class="muted">{{companyAddress}} | {{companyContact}} | GSTIN {{companyGstin}}</div>
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
</html>', updated_at = now()
WHERE setting_key = 'PRODUCTION_REPORT_TEMPLATE';
