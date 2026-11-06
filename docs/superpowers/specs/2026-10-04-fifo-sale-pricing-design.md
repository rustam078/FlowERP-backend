# FIFO Sale Pricing + Order-level Discount

Date: 2026-10-04
Status: Approved (step 1 of a larger sale-form rework; later steps to follow)

## Goal

A typed (non-scanned) sale line is no longer charged one hand-typed price. Its price is
derived from the inventory batches it actually consumes, in FIFO order, each batch at its
own selling price. One product still renders as one line (weighted-average price, exact
FIFO total). The per-line discount is removed and replaced by a single order-level
discount (flat currency) near the order total.

Scanned QR lines are out of scope: they keep their frozen printed price.

## Price resolution (per batch)

`effectiveBatchPrice = batch.sellingPrice != null ? batch.sellingPrice : currentSalePrice(product)`
— the same rule already used by the QR label and the batch table (`InventoryService`).
If neither exists for a drawn batch, fail with "Set a sale price for <product>...".

## Backend

### Request DTOs
- `CreateSaleOrderRequest` gains `discount` (order-level, flat currency, optional).
- `CreateSaleOrderItemRequest.sellingPrice` becomes optional (ignored for typed lines,
  still read for scanned lines where it is the frozen printed price). Per-line `discount`
  field stays for compatibility but is no longer used.

### `SalesService.createSale` — three phases
1. **Plan** each line without DB writes:
   - Typed: walk `findAvailableBatchesForSale` (FIFO, `receivedDate ASC`), capture the
     draw list `[{batch, qty, unitCost, price}]`; line gross = Σ(qty × price); avg price
     = gross / qty; total cost = Σ(qty × unitCost).
   - Scanned: unchanged, gross = printedPrice × qty.
2. **Distribute** the order discount across lines proportional to gross (largest-remainder
   so the pennies sum exactly to the entered discount). This keeps
   `SalesOrderItem.lineTotal = gross − allocatedDiscount`, so the dashboard profit SQL,
   the per-line invoice discount column and per-line profit all stay correct with no other
   change. `order.discount` = the entered amount.
3. **Persist** from the captured plan (single batch read, preserves the pessimistic lock
   and the reconciliation pre-check): deduct inventory, save order items with avg selling
   price + allocated discount.

- `net = subtotal − discount`; GST on net (unchanged); `grandTotal = net + GST`.
- Sale details total profit = Σ(line profit) − order discount.

### New endpoint (live preview)
`GET /api/inventory/product/{productId}/sale-batches`
→ `List<SaleBatchResponse> [{batchNumber, quantityAvailable, sellingPrice, unitCost}]`
FIFO-ordered, effective price resolved — so the form preview matches the charge exactly.

## Frontend (`CreateSale.jsx`)
- On adding a typed product, fetch its sale-batches and store on the line.
- Price column becomes read-only: FIFO weighted-average with a small `10×₹10 + 5×₹20`
  breakdown that updates as qty changes. Scanned lines show their single frozen price.
- Remove the per-line Discount column; add one Discount (₹) field in Order Summary,
  clamped to subtotal.
- Totals: subtotal = Σ line gross; net = subtotal − discount; GST on net; payable = net +
  GST; est. profit = Σ line profit − discount.
- Payload: items send `{productId, quantity, batchNumber?, serials?, sellingPrice(scanned
  only)}` plus order-level `discount`.

## Error handling
- Typed qty > Σ available → existing stock error (UI also caps qty at Σ available).
- Drawn batch with no price and no product price → "Set a sale price..." error.
- Discount > subtotal → clamped in UI, validated server-side.
- Scanned + reconciliation flow untouched.

## Testing
e2e on a throwaway DB: two batches (10@₹10, 20@₹20), sell 15 → line total ₹200, avg
₹13.33, stock 10→0 and 20→15; with order discount ₹50 → net ₹150 + GST; confirm dashboard
profit = revenue − cost − discount. Delete only the rows created by the test afterwards.
