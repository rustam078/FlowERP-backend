-- Static reference to the invoice whose checkout made this payment (for cross-invoice due clearance).
ALTER TABLE customer_payment ADD COLUMN reference_invoice_no VARCHAR(50);
