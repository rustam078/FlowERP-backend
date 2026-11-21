-- Vendors and investments (purchases / overheads) with their items and payments.

CREATE TABLE vendor (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    name varchar(150) NOT NULL,
    contact_name varchar(150),
    mobile varchar(20) NOT NULL,
    email varchar(150),
    gst_number varchar(30),
    address text,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp,
    is_deleted boolean DEFAULT false NOT NULL,
    CONSTRAINT vendor_pkey PRIMARY KEY (id),
    CONSTRAINT fk_vendor_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);
CREATE INDEX idx_vendor_active ON vendor USING btree (active);
CREATE INDEX idx_vendor_created_at ON vendor USING btree (created_at);
CREATE UNIQUE INDEX uk_vendor_email ON vendor USING btree (portfolio_id, email) WHERE ((email IS NOT NULL) AND (is_deleted = false));
CREATE UNIQUE INDEX uk_vendor_gst ON vendor USING btree (portfolio_id, gst_number) WHERE ((gst_number IS NOT NULL) AND (is_deleted = false));
CREATE UNIQUE INDEX uk_vendor_mobile ON vendor USING btree (portfolio_id, mobile) WHERE (is_deleted = false);
CREATE UNIQUE INDEX uk_vendor_name ON vendor USING btree (portfolio_id, lower((name)::text)) WHERE (is_deleted = false);

CREATE TABLE investment (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    invoice_number varchar(100) NOT NULL,
    vendor_id uuid,
    investment_type varchar(30) NOT NULL,
    purchase_date date NOT NULL,
    sub_total numeric(18,2) DEFAULT 0 NOT NULL,
    gst_amount numeric(18,2) DEFAULT 0 NOT NULL,
    discount_amount numeric(18,2) DEFAULT 0 NOT NULL,
    other_charge numeric(18,2) DEFAULT 0 NOT NULL,
    grand_total numeric(18,2) DEFAULT 0 NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp,
    is_deleted boolean DEFAULT false NOT NULL,
    CONSTRAINT investment_pkey PRIMARY KEY (id),
    CONSTRAINT uk_investment_invoice_number UNIQUE (portfolio_id, invoice_number),
    CONSTRAINT fk_investment_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),
    CONSTRAINT fk_investment_vendor FOREIGN KEY (vendor_id) REFERENCES vendor(id)
);
CREATE INDEX idx_investment_portfolio_date ON investment USING btree (portfolio_id, purchase_date);
CREATE INDEX idx_investment_type ON investment USING btree (investment_type);
CREATE INDEX idx_investment_vendor ON investment USING btree (vendor_id);

CREATE TABLE investment_item (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    investment_id uuid NOT NULL,
    item_type varchar(30) NOT NULL,
    product_id uuid,
    item_name varchar(200) NOT NULL,
    quantity numeric(18,2) NOT NULL,
    unit varchar(30) NOT NULL,
    rate numeric(18,2) NOT NULL,
    total_amount numeric(18,2) NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp,
    is_deleted boolean DEFAULT false NOT NULL,
    selling_price numeric(14,2),
    CONSTRAINT investment_item_pkey PRIMARY KEY (id),
    CONSTRAINT fk_investment_item_investment FOREIGN KEY (investment_id) REFERENCES investment(id),
    CONSTRAINT fk_investment_item_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),
    CONSTRAINT fk_investment_item_product FOREIGN KEY (product_id) REFERENCES product(id)
);
CREATE INDEX idx_investment_item_product ON investment_item USING btree (product_id);
CREATE INDEX idx_investment_item_type ON investment_item USING btree (item_type);

CREATE TABLE investment_payment (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    investment_id uuid NOT NULL,
    payment_date date NOT NULL,
    mode varchar(20) NOT NULL,
    amount numeric(18,2) NOT NULL,
    reference varchar(120),
    created_at timestamp NOT NULL,
    updated_at timestamp,
    payment_group_id uuid,
    CONSTRAINT investment_payment_pkey PRIMARY KEY (id),
    CONSTRAINT fk_investment_payment_investment FOREIGN KEY (investment_id) REFERENCES investment(id) ON DELETE CASCADE,
    CONSTRAINT fk_investment_payment_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);
CREATE INDEX idx_investment_payment_investment ON investment_payment USING btree (investment_id);
CREATE INDEX idx_investment_payment_portfolio_date ON investment_payment USING btree (portfolio_id, payment_date, created_at);

