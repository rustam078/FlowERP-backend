-- Employment spells: salary accrues only during an open-or-closed work period. Deactivating an
-- employee closes the open spell at the last working date; reactivating opens a new spell. This
-- stops payroll from generating full-salary slips for months after someone has left / gone on leave.

CREATE TABLE IF NOT EXISTS employee_work_period (
    id          UUID PRIMARY KEY,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    start_date  DATE NOT NULL,
    end_date    DATE,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_work_period_emp ON employee_work_period (employee_id);

-- Backfill: give every existing employee an open spell from their joining date, so current
-- payroll is unchanged until they are explicitly deactivated with a last-working date.
INSERT INTO employee_work_period (id, employee_id, start_date, end_date, created_at, updated_at)
SELECT gen_random_uuid(), e.id, COALESCE(e.joining_date, CURRENT_DATE), NULL, now(), now()
FROM employee e
WHERE NOT EXISTS (SELECT 1 FROM employee_work_period w WHERE w.employee_id = e.id);
