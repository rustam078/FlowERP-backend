-- Credit sales: track how much of each sale is paid; the remainder is the customer's due.
ALTER TABLE sales_order ADD COLUMN IF NOT EXISTS amount_paid numeric(19, 2) NOT NULL DEFAULT 0;

-- Existing sales were all fully paid.
UPDATE sales_order SET amount_paid = grand_total;

-- Payments received against sales (one UI payment can span several sales via payment_group_id).
CREATE TABLE IF NOT EXISTS customer_payment (
    id uuid NOT NULL,
    sales_order_id uuid NOT NULL,
    payment_date date NOT NULL,
    mode character varying(20) NOT NULL,
    amount numeric(18, 2) NOT NULL,
    payment_group_id uuid,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone,
    CONSTRAINT customer_payment_pkey PRIMARY KEY (id),
    CONSTRAINT fk_customer_payment_sale FOREIGN KEY (sales_order_id)
        REFERENCES sales_order (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_customer_payment_sale ON customer_payment (sales_order_id);
