-- Step 4 (Payroll): link a ledger transaction to the month (pay-slip) it settles.
ALTER TABLE employee_ledger ADD COLUMN IF NOT EXISTS period DATE;
CREATE INDEX IF NOT EXISTS idx_ledger_emp_period ON employee_ledger (employee_id, period);
