-- Products, their pricing history, piece codes, QR units and inventory batches.
-- Batch numbers come from each portfolio's own counter (portfolio.batch_prefix + batch_seq).

CREATE TABLE product (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    name varchar(100) NOT NULL,
    description varchar(500),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    icon_name varchar(100),
    source varchar(30) DEFAULT 'MANUFACTURED'::character varying NOT NULL,
    CONSTRAINT product_pkey PRIMARY KEY (id),
    CONSTRAINT uk_product_name UNIQUE (portfolio_id, name),
    CONSTRAINT fk_product_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);

CREATE TABLE product_image (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    product_id uuid NOT NULL,
    file_name varchar(255),
    content_type varchar(100),
    size_bytes bigint,
    data bytea NOT NULL,
    created_at timestamp,
    updated_at timestamp,
    CONSTRAINT product_image_pkey PRIMARY KEY (id),
    CONSTRAINT product_image_product_id_key UNIQUE (product_id),
    CONSTRAINT fk_product_image_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),
    CONSTRAINT product_image_product_id_fkey FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE TABLE product_rate (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    product_id uuid NOT NULL,
    rate numeric(12,2) NOT NULL,
    effective_from date NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT product_rate_pkey PRIMARY KEY (id),
    CONSTRAINT uk_product_rate_effective_date UNIQUE (product_id, effective_from),
    CONSTRAINT fk_product_rate_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),
    CONSTRAINT fk_product_rate_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE product_material_cost (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    product_id uuid NOT NULL,
    cost numeric(18,2) NOT NULL,
    effective_from date NOT NULL,
    remarks text,
    created_at timestamp NOT NULL,
    updated_at timestamp,
    sale_price numeric(18,2),
    CONSTRAINT product_material_cost_pkey PRIMARY KEY (id),
    CONSTRAINT uk_product_material_cost UNIQUE (product_id, effective_from),
    CONSTRAINT fk_product_material_cost_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),
    CONSTRAINT fk_product_material_cost_product FOREIGN KEY (product_id) REFERENCES product(id)
);
CREATE INDEX idx_product_material_cost_effective ON product_material_cost USING btree (effective_from);
CREATE INDEX idx_product_material_cost_product ON product_material_cost USING btree (product_id);

CREATE TABLE product_piece_code (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    product_id uuid NOT NULL,
    code varchar(100) NOT NULL,
    description varchar(255),
    rate numeric(19,2) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp,
    created_by varchar(100),
    updated_by varchar(100),
    CONSTRAINT product_piece_code_pkey PRIMARY KEY (id),
    CONSTRAINT uk_product_piece_code UNIQUE (portfolio_id, code),
    CONSTRAINT fk_piece_code_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_product_piece_code_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);

CREATE TABLE product_unit (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    serial varchar(40) NOT NULL,
    batch_number varchar(40) NOT NULL,
    product_id uuid NOT NULL,
    printed_price numeric(12,2) NOT NULL,
    status varchar(20) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    sale_order_id uuid,
    sold_at timestamp,
    created_at timestamp NOT NULL,
    updated_at timestamp,
    CONSTRAINT product_unit_pkey PRIMARY KEY (id),
    CONSTRAINT uk_product_unit_serial UNIQUE (portfolio_id, serial),
    CONSTRAINT product_unit_status_check CHECK (((status)::text = ANY (ARRAY[('AVAILABLE'::character varying)::text, ('SOLD'::character varying)::text, ('VOID'::character varying)::text]))),
    CONSTRAINT fk_product_unit_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),
    CONSTRAINT fk_product_unit_product FOREIGN KEY (product_id) REFERENCES product(id)
);
CREATE INDEX idx_product_unit_batch ON product_unit USING btree (batch_number);
CREATE INDEX idx_product_unit_status ON product_unit USING btree (status);

CREATE TABLE inventory_batch (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    batch_number varchar(50) NOT NULL,
    product_id uuid NOT NULL,
    source varchar(20) NOT NULL,
    source_id uuid NOT NULL,
    received_date date NOT NULL,
    quantity_received numeric(12,2) NOT NULL,
    quantity_available numeric(12,2) NOT NULL,
    unit_cost numeric(12,2) NOT NULL,
    total_cost numeric(12,2) NOT NULL,
    status varchar(20) NOT NULL,
    remarks varchar(500),
    created_at timestamp,
    updated_at timestamp,
    selling_price numeric(12,2),
    CONSTRAINT inventory_batch_pkey PRIMARY KEY (id),
    CONSTRAINT uk_inventory_batch_number UNIQUE (portfolio_id, batch_number),
    CONSTRAINT inventory_batch_status_check CHECK (((status)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('SOLD'::character varying)::text, ('CLOSED'::character varying)::text]))),
    CONSTRAINT fk_inventory_batch_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),
    CONSTRAINT fk_inventory_batch_product FOREIGN KEY (product_id) REFERENCES product(id)
);

