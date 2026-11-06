# Credit terminology fix + cross-invoice clearance reference

Date: 2026-10-10
Status: approved (fresh DB — no data-backfill migration needed)

## Problem
"CREDIT" exists as both a `PaymentMode` (bought fully on account / udhaari) and a
`PaymentStatus` (still owes money). Same word, two meanings → confusion. Also, when a
later sale's payment clears an older invoice's due, the older invoice gives no visible
record of *when / by which mode / from which invoice* it was cleared.

## Decision
- Keep `PaymentMode.CREDIT` (needed for filtering "all udhaari sales"); display it as
  **"Udhaar (Credit)"**.
- Rename `PaymentStatus.CREDIT` → **`DUE`**. Status is now only `PAID` / `DUE`.
- Record the clearing invoice number on each cross-invoice payment as a **static text**
  value (no FK/mapping), and show it on the cleared invoice's details page + receipt.
- Sale form: do **not** auto-fill "Amount received". Cashier types it, or clicks "Pay full".

## Part A — Status CREDIT → DUE
Backend
- `PaymentStatus`: `CREDIT` → `DUE`.
- `SalesService.statusOf()` returns `DUE`; order-create placeholder uses `DUE`.
Frontend
- Status filter lists → `['All','PAID','DUE']`; chip color `DUE: warning`.
- Payment-mode display helper maps `CREDIT` → "Udhaar (Credit)".

## Part B — Clearance reference (static)
Backend
- Migration **V9**: `ALTER TABLE customer_payment ADD COLUMN reference_invoice_no VARCHAR(50)`.
- `CustomerPayment.referenceInvoiceNo` (String, static — no relation).
- `payOneSale`: when the paid invoice ≠ the checkout invoice, set
  `referenceInvoiceNo = <checkout invoice no>`. Uses a `PaymentContext` param object
  (mode + groupId + sourceInvoiceNo) to stay within the 4-param limit.
- `SalesDetailsResponse.clearances`: list of {date, mode, amount, referenceInvoiceNo}
  built from this invoice's payments that carry a referenceInvoiceNo.
- `InvoiceResponse` + receipt `paymentRows`: render the same clearance lines in the PDF.
Frontend
- SaleDetails → Details tab: "Due cleared later" section listing each clearance as
  `₹1,500 — 11 Oct 2026, Cash · Ref: INV-2`.

## Part C — Sale form: no auto-fill
- Remove the effect that defaults "Amount received" to the payable bill.
- Default empty/0; cashier enters manually or clicks the existing "Pay full" button.

## Out of scope
No data backfill (DB is fresh). No standalone receive-payment screen.
