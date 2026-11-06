-- Step 3: exception-based attendance — only absent / half-day records are stored.
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
