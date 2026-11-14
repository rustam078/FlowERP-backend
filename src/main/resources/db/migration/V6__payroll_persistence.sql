-- Payroll persistence + inactive-spell integrity.
-- employee_work_period rows are INACTIVE spells: start_date = inactive date (first non-working day),
-- end_date = reactivation date (a working day again); end_date stays NULL while still inactive.

ALTER TABLE employee_work_period
    ADD CONSTRAINT chk_work_period_dates CHECK (end_date IS NULL OR end_date > start_date);

-- At most one open (still inactive) spell per employee.
CREATE UNIQUE INDEX IF NOT EXISTS uq_work_period_open
    ON employee_work_period (employee_id) WHERE end_date IS NULL;

-- Spells are always read per employee in date order.
DROP INDEX IF EXISTS idx_work_period_emp;
CREATE INDEX IF NOT EXISTS idx_work_period_emp_start ON employee_work_period (employee_id, start_date);

-- One saved salary slip per employee per completed month (the current month is never stored).
-- Unfinalized rows are re-snapshotted as payments/attendance change; finalized (fully paid) rows are locked.
CREATE TABLE IF NOT EXISTS employee_payslip (
    id              UUID PRIMARY KEY,
    employee_id     UUID          NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    period          DATE          NOT NULL,
    month_status    VARCHAR(10)   NOT NULL,
    active_days     INT           NOT NULL,
    month_days      INT           NOT NULL,
    monthly_salary  NUMERIC(18,2) NOT NULL,
    base_salary     NUMERIC(18,2) NOT NULL,
    leave_days      NUMERIC(5,1)  NOT NULL,
    leave_deduction NUMERIC(18,2) NOT NULL,
    earned          NUMERIC(18,2) NOT NULL,
    paid            NUMERIC(18,2) NOT NULL,
    due             NUMERIC(18,2) NOT NULL,
    payment_status  VARCHAR(10),
    finalized       BOOLEAN       NOT NULL DEFAULT FALSE,
    finalized_at    TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    CONSTRAINT uq_payslip_emp_period UNIQUE (employee_id, period),
    CONSTRAINT chk_payslip_first_day CHECK (EXTRACT(DAY FROM period) = 1),
    CONSTRAINT chk_payslip_month_status CHECK (month_status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_payslip_days CHECK (active_days BETWEEN 0 AND month_days)
);
