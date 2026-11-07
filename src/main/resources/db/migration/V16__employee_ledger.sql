-- Step 4 (Payroll): employee money account — advances given and settlement payouts.
CREATE TABLE IF NOT EXISTS employee_ledger (
    id          UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    entry_date  DATE NOT NULL,
    type        VARCHAR(20) NOT NULL,
    amount      NUMERIC(18,2) NOT NULL,
    mode        VARCHAR(20),
    note        VARCHAR(200),
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ledger_emp_date ON employee_ledger (employee_id, entry_date);
