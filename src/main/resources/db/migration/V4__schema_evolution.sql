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
