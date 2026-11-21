-- Customers, sales, customer payments, and stock movements (linked to sale items).

CREATE TABLE customer (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    name varchar(255) NOT NULL,
    mobile varchar(15) NOT NULL,
    email varchar(255),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT customer_pkey PRIMARY KEY (id),
    CONSTRAINT uk_customer_mobile UNIQUE (portfolio_id, mobile),
    CONSTRAINT fk_customer_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);

CREATE TABLE sales_order (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    invoice_no varchar(50) NOT NULL,
    customer_name varchar(255) NOT NULL,
    customer_mobile varchar(20) NOT NULL,
    customer_email varchar(255),
    subtotal numeric(19,2) NOT NULL,
    discount numeric(19,2) DEFAULT 0 NOT NULL,
    tax numeric(19,2) DEFAULT 0 NOT NULL,
    grand_total numeric(19,2) NOT NULL,
    payment_mode varchar(30) NOT NULL,
    payment_provider varchar(100),
    payment_status varchar(30) NOT NULL,
    remarks text,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    amount_paid numeric(19,2) DEFAULT 0 NOT NULL,
    amount_received numeric(19,2) DEFAULT 0 NOT NULL,
    CONSTRAINT sales_order_pkey PRIMARY KEY (id),
    CONSTRAINT uk_sales_order_invoice_no UNIQUE (portfolio_id, invoice_no),
    CONSTRAINT fk_sales_order_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);
CREATE INDEX idx_sales_order_mobile ON sales_order USING btree (customer_mobile);
CREATE INDEX idx_sales_order_portfolio_created ON sales_order USING btree (portfolio_id, created_at);

CREATE TABLE sales_order_item (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    sales_order_id uuid NOT NULL,
    product_id uuid NOT NULL,
    quantity numeric(19,2) NOT NULL,
    unit_price numeric(19,2) NOT NULL,
    selling_price numeric(19,2) NOT NULL,
    discount numeric(19,2) DEFAULT 0 NOT NULL,
    line_total numeric(19,2) NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    batch_number varchar(40),
    CONSTRAINT sales_order_item_pkey PRIMARY KEY (id),
    CONSTRAINT fk_sales_item_order FOREIGN KEY (sales_order_id) REFERENCES sales_order(id),
    CONSTRAINT fk_sales_item_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_sales_order_item_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);
CREATE INDEX idx_sales_item_order ON sales_order_item USING btree (sales_order_id);
CREATE INDEX idx_sales_item_product ON sales_order_item USING btree (product_id);

CREATE TABLE customer_payment (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    sales_order_id uuid NOT NULL,
    payment_date date NOT NULL,
    mode varchar(20) NOT NULL,
    amount numeric(18,2) NOT NULL,
    payment_group_id uuid,
    reference_invoice_no varchar(50),
    created_at timestamp NOT NULL,
    updated_at timestamp,
    CONSTRAINT customer_payment_pkey PRIMARY KEY (id),
    CONSTRAINT fk_customer_payment_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),
    CONSTRAINT fk_customer_payment_sale FOREIGN KEY (sales_order_id) REFERENCES sales_order(id) ON DELETE CASCADE
);
CREATE INDEX idx_customer_payment_portfolio_date ON customer_payment USING btree (portfolio_id, payment_date, created_at);
CREATE INDEX idx_customer_payment_sale ON customer_payment USING btree (sales_order_id);

CREATE TABLE inventory_transaction (
    id uuid NOT NULL,
    portfolio_id uuid NOT NULL,
    sales_order_item_id uuid NOT NULL,
    product_id uuid NOT NULL,
    batch_id uuid NOT NULL,
    transaction_type varchar(30) NOT NULL,
    quantity numeric(19,2) NOT NULL,
    unit_cost numeric(19,2) NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    reason varchar(200),
    CONSTRAINT inventory_transaction_pkey PRIMARY KEY (id),
    CONSTRAINT fk_inventory_transaction_batch FOREIGN KEY (batch_id) REFERENCES inventory_batch(id),
    CONSTRAINT fk_inventory_transaction_item FOREIGN KEY (sales_order_item_id) REFERENCES sales_order_item(id),
    CONSTRAINT fk_inventory_transaction_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id),
    CONSTRAINT fk_inventory_transaction_product FOREIGN KEY (product_id) REFERENCES product(id)
);
CREATE INDEX idx_inventory_transaction_portfolio ON inventory_transaction USING btree (portfolio_id);
CREATE INDEX idx_inventory_tx_batch ON inventory_transaction USING btree (batch_id);
CREATE INDEX idx_inventory_tx_item ON inventory_transaction USING btree (sales_order_item_id);
CREATE INDEX idx_inventory_tx_product ON inventory_transaction USING btree (product_id);

