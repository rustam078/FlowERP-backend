# QR Unit-Labelling & Scan-to-Sell — Design

Date: 2026-09-13
Status: Approved (build backend first, then frontend)

## Goal
Generate a unique QR label per physical garment in a batch, print & paste them, then
scan (USB/Bluetooth wedge **or** camera) to auto-add items to a sale. Each piece is
charged the price **frozen on its tag** and deducts **its own batch**; a piece can't be
sold twice.

## Decisions (from brainstorming)
- **Per-unit serials** (not identical batch QRs). Serial = `<batchNumber>-<NNN>` e.g. `BT000087-001`.
- **Both** scan methods: keyboard-wedge scan box + in-app camera scanner.
- **Frozen price**: printed price is snapshotted per unit at generation; scan uses it (staff can override).
- **Merge** same batch+price scans into one line (qty n, serials tracked); different batch/price = new line.
- **Typed (non-scanned) sales** keep working as today (FIFO, current price) even on serialized batches.
- Feature lives in its **own module** (backend `com.skr.erp.qr`, frontend `src/modules/qr/`).

## Data model
New table `product_unit` (migration V30):
- `id` UUID, `serial` (unique), `batch_number`, `product_id`, `printed_price` NUMERIC,
  `status` (`AVAILABLE`/`SOLD`/`VOID`), `sale_order_id` (nullable), `sold_at` (nullable),
  `created_at`/`updated_at`.
- Also add nullable `batch_number` to `sales_order_item`.
- Traceability: which serials a sale consumed is recoverable via `product_unit.sale_order_id`.

## QR payload
Versioned, delimited, self-describing: `SKR1|<serial>|<productName>|<price>`.
App validates `SKR1`, uses `serial` as source of truth (server lookup gives authoritative frozen price).

## Backend API (`/api/qr`)
- `POST /api/qr/batches/{batchNumber}/units/generate` — create units for a batch (default count =
  quantityAvailable; continues from highest existing serial index; blocked if no sale price set). Returns unit list.
- `GET  /api/qr/batches/{batchNumber}/units` — list units for a batch (for the print sheet / reprint).
- `GET  /api/qr/units/{serial}` — scan lookup → `{serial, productId, productName, batchNumber, printedPrice, status}`;
  409 if not AVAILABLE.
- `PATCH /api/qr/units/{serial}/void` — mark a damaged/lost unit VOID.

## Generation rules
- Requires a current sale price for the product (else 400 with clear message).
- `printed_price` = current effective sale price at generation time (frozen).
- Serial index continues from the batch's current max, so re-generating never duplicates.

## Scan → cart (frontend, phase 2)
- Wedge: focused Scan box; payload + Enter → parse → lookup → add.
- Camera: modal decoder → same lookup → add.
- Add rules: AVAILABLE → add at frozen price; same batch+price in cart → qty+1 & append serial;
  different batch/price → new line; SOLD/VOID/already-in-cart → reject with toast.

## Save Sale — deduction
- Sale item DTO gains optional `batchNumber` + `serials[]`.
- Scanned line: deduct that exact batch, mark its units SOLD + set `sale_order_id`/`sold_at`.
- Typed line: unchanged FIFO + current price.
- Save-time validation: each serial still AVAILABLE and matches its line's batch/product.

## Build phases
1. Generate & print (units table + generate/list endpoints + Batch History button + printable QR sheet).
2. Scan → cart (lookup endpoint + Scan box + camera, wired into both sale forms).
3. Save & deduct (batch-accurate deduction + mark-sold + double-sell guard + void/reprint).

Order of work: full backend first, then frontend.

## Known trade-off
Allowing typed FIFO sales on serialized batches can let unit records and batch qty drift; accepted for
now, a later reconciliation report can flag it.
