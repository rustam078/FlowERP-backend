-- Step 3: employee pay model — piece-rate vs monthly salary, plus an opening advance.
ALTER TABLE employee ADD COLUMN IF NOT EXISTS payment_type VARCHAR(20) NOT NULL DEFAULT 'PIECE_RATE';
ALTER TABLE employee ADD COLUMN IF NOT EXISTS monthly_salary NUMERIC(12,2);
ALTER TABLE employee ADD COLUMN IF NOT EXISTS opening_advance NUMERIC(12,2);
