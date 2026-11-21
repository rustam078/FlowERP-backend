-- Employees (attendance, payroll ledger, payslips, inactive periods) and piece-work production.

CREATE TABLE employee (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    employee_code varchar(50) NOT NULL,
    full_name varchar(200) NOT NULL,
    mobile_number varchar(20),
    address text,
    joining_date date NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    email varchar(255),
    payment_type varchar(20) DEFAULT 'PIECE_RATE'::character varying NOT NULL,
    monthly_salary numeric(12,2),
    advance_balance numeric(18,2) DEFAULT 0 NOT NULL,
    CONSTRAINT employee_pkey PRIMARY KEY (id),
    CONSTRAINT uk_employee_code UNIQUE (portfolio_id, employee_code),
    CONSTRAINT uk_employee_email UNIQUE (portfolio_id, email),
    CONSTRAINT fk_employee_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);
CREATE INDEX idx_employee_name ON employee USING btree (full_name);

CREATE TABLE employee_attendance (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    date date NOT NULL,
    status varchar(20) NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT employee_attendance_pkey PRIMARY KEY (id),
    CONSTRAINT uq_attendance_emp_date UNIQUE (employee_id, date),
    CONSTRAINT employee_attendance_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_attendance_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);
CREATE INDEX idx_attendance_emp_date ON employee_attendance USING btree (employee_id, date);

CREATE TABLE employee_ledger (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    entry_date date NOT NULL,
    type varchar(20) NOT NULL,
    amount numeric(18,2) NOT NULL,
    mode varchar(20),
    note varchar(200),
    period date,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT employee_ledger_pkey PRIMARY KEY (id),
    CONSTRAINT employee_ledger_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_ledger_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);
CREATE INDEX idx_employee_ledger_portfolio_date ON employee_ledger USING btree (portfolio_id, entry_date, created_at);
CREATE INDEX idx_ledger_emp_date ON employee_ledger USING btree (employee_id, entry_date);
CREATE INDEX idx_ledger_emp_period ON employee_ledger USING btree (employee_id, period);

CREATE TABLE employee_payslip (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    period date NOT NULL,
    month_status varchar(10) NOT NULL,
    active_days integer NOT NULL,
    month_days integer NOT NULL,
    monthly_salary numeric(18,2) NOT NULL,
    base_salary numeric(18,2) NOT NULL,
    leave_days numeric(5,1) NOT NULL,
    leave_deduction numeric(18,2) NOT NULL,
    earned numeric(18,2) NOT NULL,
    paid numeric(18,2) NOT NULL,
    due numeric(18,2) NOT NULL,
    payment_status varchar(10),
    finalized boolean DEFAULT false NOT NULL,
    finalized_at timestamp,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT employee_payslip_pkey PRIMARY KEY (id),
    CONSTRAINT uq_payslip_emp_period UNIQUE (employee_id, period),
    CONSTRAINT chk_payslip_days CHECK (((active_days >= 0) AND (active_days <= month_days))),
    CONSTRAINT chk_payslip_first_day CHECK ((EXTRACT(day FROM period) = (1)::numeric)),
    CONSTRAINT chk_payslip_month_status CHECK (((month_status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[]))),
    CONSTRAINT employee_payslip_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_payslip_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);

CREATE TABLE employee_work_period (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    start_date date NOT NULL,
    end_date date,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT employee_work_period_pkey PRIMARY KEY (id),
    CONSTRAINT chk_work_period_dates CHECK (((end_date IS NULL) OR (end_date > start_date))),
    CONSTRAINT employee_work_period_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_work_period_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);
CREATE INDEX idx_work_period_emp_start ON employee_work_period USING btree (employee_id, start_date);
CREATE UNIQUE INDEX uq_work_period_open ON employee_work_period USING btree (employee_id) WHERE (end_date IS NULL);

CREATE TABLE production_entry (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    production_date date NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT production_entry_pkey PRIMARY KEY (id),
    CONSTRAINT fk_production_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_production_entry_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);
CREATE INDEX idx_production_entry_portfolio_date ON production_entry USING btree (portfolio_id, production_date);

CREATE TABLE production_entry_detail (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    production_entry_id uuid NOT NULL,
    product_id uuid,
    quantity integer NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    rate_snapshot numeric(12,2),
    amount_snapshot numeric(12,2),
    product_name_snapshot varchar(255),
    piece_code_id uuid,
    piece_code_snapshot varchar(100),
    inventory_batch_id uuid,
    work varchar(255),
    CONSTRAINT production_entry_detail_pkey PRIMARY KEY (id),
    CONSTRAINT fk_production_detail_entry FOREIGN KEY (production_entry_id) REFERENCES production_entry(id) ON DELETE CASCADE,
    CONSTRAINT fk_production_detail_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_production_entry_detail_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),
    CONSTRAINT fk_production_piece_code FOREIGN KEY (piece_code_id) REFERENCES product_piece_code(id)
);

