# Customer Credit (Udhaari) — Sale Payments & Dues

Date: 2026-10-04
Status: Approved. Mirrors the investment vendor-ledger pattern.

## Goal
Let a sale be paid partially (or not at all); the unpaid part becomes the customer's
**due**. When the same customer buys again, their previous outstanding is shown, and the
amount they pay now clears old dues first (oldest invoice first), then today's bill. Each
sale shows a payment status; the customer's recent transactions are viewable on demand.

Approved choices: allocation is **oldest-first across the customer's whole ledger**
(investment-style); a standalone "receive payment without a sale" is a **later** step
(the table built here supports it).

## Data (migration V6)
- `sales_order.amount_paid numeric(19,2) NOT NULL DEFAULT 0`; existing rows set to
  `grand_total` (they were fully paid). `amount_due = grand_total − amount_paid` (derived).
- New table `customer_payment` (like `investment_payment`): `id, sales_order_id (FK
  ON DELETE CASCADE), payment_date, mode, amount, payment_group_id, created_at, updated_at`.
  Rows from one UI payment share a `payment_group_id`.
- Customer outstanding = `SUM(grand_total − amount_paid)` over the customer's sales
  (grouped by `customer_mobile`, the existing unique key). No balance stored on `customer`.

## Backend
- `SalesOrder` += `amountPaid`. New entity `CustomerPayment` + `CustomerPaymentRepository`.
- `SalesOrderRepository`: `findOutstandingByMobile(mobile)` =
  `WHERE customer_mobile = :m AND amountPaid < grandTotal ORDER BY createdAt ASC`;
  reuse `findByCustomerMobileOrderByCreatedAtDesc` for history.
- `createSale`: request gains `amountReceived` (optional; null ⇒ full = fully paid, no
  credit) + existing `paymentMode`. After the order + items are saved:
  1. ledger = old outstanding sales (oldest first) + the new sale appended last.
  2. allocate `amountReceived` oldest-first: each sale `pay = min(remaining, due)`,
     create a `CustomerPayment` row (shared group id), bump `amountPaid`, set status.
  3. `amountReceived` clamped to `oldOutstanding + newGrandTotal`; more ⇒ BusinessException.
  - Status: `paid ≥ grand ⇒ PAID`, `paid == 0 ⇒ PENDING`, else `PARTIALLY_PAID`
    (existing enum, no new value).
- Endpoints (SalesController):
  - `GET /api/sales/customer/outstanding?mobile=` → `{totalOutstanding, unpaidCount}`.
  - `GET /api/sales/customer/history?mobile=&page=&size=` → paged recent sales
    `{invoiceNo, date, grandTotal, amountPaid, amountDue, status}` (lazy).
- DTOs: `CreateSaleOrderResponse`, `SalesListResponse`, `SalesDetailsResponse`,
  `CustomerSaleResponse` gain `amountPaid`/`amountDue`; new `CustomerOutstandingResponse`.

## Frontend
- `CreateSale`: on customer select, fetch outstanding. Payment section shows **Previous
  due ₹X** (if any), **Today ₹Y**, **Grand total ₹(X+Y)** (when old due > 0), an **Amount
  received** input (default = today's payable; max = X+Y), a live **Remaining due** and a
  status chip (Paid / Partial / Credit). Sends `amountReceived` + `paymentMode`.
- `SalesList`: status chip shows Paid / Partial / **Due** with the due amount.
- `SaleDetails`: a **"Customer History" tab** that lazy-loads the customer's last 10
  transactions only when opened.
- Services: `getCustomerOutstanding(mobile)`, `getCustomerHistory(mobile, page, size)`.

## Error handling
- `amountReceived` < 0 or > (old + today) ⇒ rejected (UI clamps too).
- No customer dues + full payment ⇒ behaves exactly like today (PAID), backward compatible.

## Future (not in this step)
Standalone **Receive Payment** (clear dues without a sale) — add a button that calls an
allocate-oldest-first endpoint using the same `customer_payment` table + status logic.
Also: customer statement / aging, SMS reminder. All build on this structure.
