

CREATE TABLE public.app_user (
    id uuid NOT NULL,
    username character varying(100) NOT NULL,
    password character varying(255) NOT NULL,
    full_name character varying(150) NOT NULL,
    role character varying(50) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE TABLE public.customer (
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    mobile character varying(15) NOT NULL,
    email character varying(255),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE TABLE public.employee (
    id uuid NOT NULL,
    employee_code character varying(50) NOT NULL,
    full_name character varying(200) NOT NULL,
    mobile_number character varying(20),
    address text,
    joining_date date NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    email character varying(255)
);

CREATE TABLE public.inventory_batch (
    id uuid NOT NULL,
    batch_number character varying(50) NOT NULL,
    product_id uuid NOT NULL,
    source character varying(20) NOT NULL,
    source_id uuid NOT NULL,
    received_date date NOT NULL,
    quantity_received numeric(12,2) NOT NULL,
    quantity_available numeric(12,2) NOT NULL,
    unit_cost numeric(12,2) NOT NULL,
    total_cost numeric(12,2) NOT NULL,
    status character varying(20) NOT NULL,
    remarks character varying(500),
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    selling_price numeric(12,2),
    CONSTRAINT inventory_batch_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SOLD'::character varying, 'CLOSED'::character varying])::text[])))
);

CREATE SEQUENCE public.inventory_batch_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.inventory_transaction (
    id uuid NOT NULL,
    sales_order_item_id uuid NOT NULL,
    product_id uuid NOT NULL,
    batch_id uuid NOT NULL,
    transaction_type character varying(30) NOT NULL,
    quantity numeric(19,2) NOT NULL,
    unit_cost numeric(19,2) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    reason character varying(200)
);

CREATE TABLE public.investment (
    id uuid NOT NULL,
    invoice_number character varying(100) NOT NULL,
    vendor_id uuid,
    investment_type character varying(30) NOT NULL,
    purchase_date date NOT NULL,
    sub_total numeric(18,2) DEFAULT 0 NOT NULL,
    gst_amount numeric(18,2) DEFAULT 0 NOT NULL,
    discount_amount numeric(18,2) DEFAULT 0 NOT NULL,
    other_charge numeric(18,2) DEFAULT 0 NOT NULL,
    grand_total numeric(18,2) DEFAULT 0 NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL
);

CREATE TABLE public.investment_item (
    id uuid NOT NULL,
    investment_id uuid NOT NULL,
    item_type character varying(30) NOT NULL,
    product_id uuid,
    item_name character varying(200) NOT NULL,
    quantity numeric(18,2) NOT NULL,
    unit character varying(30) NOT NULL,
    rate numeric(18,2) NOT NULL,
    total_amount numeric(18,2) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL
);

CREATE TABLE public.investment_payment (
    id uuid NOT NULL,
    investment_id uuid NOT NULL,
    payment_date date NOT NULL,
    mode character varying(20) NOT NULL,
    amount numeric(18,2) NOT NULL,
    reference character varying(120),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone
);

CREATE TABLE public.product (
    id uuid NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(500),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    icon_name character varying(100),
    source character varying(30) DEFAULT 'MANUFACTURED'::character varying NOT NULL
);

CREATE TABLE public.product_image (
    id uuid NOT NULL,
    product_id uuid NOT NULL,
    file_name character varying(255),
    content_type character varying(100),
    size_bytes bigint,
    data bytea NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE TABLE public.product_material_cost (
    id uuid NOT NULL,
    product_id uuid NOT NULL,
    cost numeric(18,2) NOT NULL,
    effective_from date NOT NULL,
    remarks text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone,
    sale_price numeric(18,2)
);

CREATE TABLE public.product_piece_code (
    id uuid NOT NULL,
    product_id uuid NOT NULL,
    code character varying(100) NOT NULL,
    description character varying(255),
    rate numeric(19,2) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone,
    created_by character varying(100),
    updated_by character varying(100)
);

CREATE TABLE public.product_rate (
    id uuid NOT NULL,
    product_id uuid NOT NULL,
    rate numeric(12,2) NOT NULL,
    effective_from date NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE TABLE public.product_unit (
    id uuid NOT NULL,
    serial character varying(40) NOT NULL,
    batch_number character varying(40) NOT NULL,
    product_id uuid NOT NULL,
    printed_price numeric(12,2) NOT NULL,
    status character varying(20) DEFAULT 'AVAILABLE'::character varying NOT NULL,
    sale_order_id uuid,
    sold_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone,
    CONSTRAINT product_unit_status_check CHECK (((status)::text = ANY ((ARRAY['AVAILABLE'::character varying, 'SOLD'::character varying, 'VOID'::character varying])::text[])))
);

CREATE TABLE public.production_entry (
    id uuid NOT NULL,
    employee_id uuid NOT NULL,
    production_date date NOT NULL,
    remarks character varying(500),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE TABLE public.production_entry_detail (
    id uuid NOT NULL,
    production_entry_id uuid NOT NULL,
    product_id uuid NOT NULL,
    quantity integer NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    rate_snapshot numeric(12,2),
    amount_snapshot numeric(12,2),
    product_name_snapshot character varying(255),
    piece_code_id uuid,
    piece_code_snapshot character varying(100),
    inventory_batch_id uuid
);

CREATE TABLE public.sales_order (
    id uuid NOT NULL,
    invoice_no character varying(50) NOT NULL,
    customer_name character varying(255) NOT NULL,
    customer_mobile character varying(20) NOT NULL,
    customer_email character varying(255),
    subtotal numeric(19,2) NOT NULL,
    discount numeric(19,2) DEFAULT 0 NOT NULL,
    tax numeric(19,2) DEFAULT 0 NOT NULL,
    grand_total numeric(19,2) NOT NULL,
    payment_mode character varying(30) NOT NULL,
    payment_provider character varying(100),
    payment_status character varying(30) NOT NULL,
    remarks text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE TABLE public.sales_order_item (
    id uuid NOT NULL,
    sales_order_id uuid NOT NULL,
    product_id uuid NOT NULL,
    quantity numeric(19,2) NOT NULL,
    unit_price numeric(19,2) NOT NULL,
    selling_price numeric(19,2) NOT NULL,
    discount numeric(19,2) DEFAULT 0 NOT NULL,
    line_total numeric(19,2) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    batch_number character varying(40)
);

CREATE TABLE public.system_setting (
    id uuid NOT NULL,
    setting_key character varying(100) NOT NULL,
    setting_value text NOT NULL,
    description character varying(500),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE TABLE public.vendor (
    id uuid NOT NULL,
    name character varying(150) NOT NULL,
    contact_name character varying(150),
    mobile character varying(20) NOT NULL,
    email character varying(150),
    gst_number character varying(30),
    address text,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone,
    is_deleted boolean DEFAULT false NOT NULL
);

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.app_user
    ADD CONSTRAINT app_user_username_key UNIQUE (username);

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_mobile_key UNIQUE (mobile);

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT customer_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT employee_employee_code_key UNIQUE (employee_code);

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT employee_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.inventory_batch
    ADD CONSTRAINT inventory_batch_batch_number_key UNIQUE (batch_number);

ALTER TABLE ONLY public.inventory_batch
    ADD CONSTRAINT inventory_batch_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.inventory_transaction
    ADD CONSTRAINT inventory_transaction_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.investment_item
    ADD CONSTRAINT investment_item_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.investment_payment
    ADD CONSTRAINT investment_payment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.investment
    ADD CONSTRAINT investment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_image
    ADD CONSTRAINT product_image_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_image
    ADD CONSTRAINT product_image_product_id_key UNIQUE (product_id);

ALTER TABLE ONLY public.product_material_cost
    ADD CONSTRAINT product_material_cost_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_name_key UNIQUE (name);

ALTER TABLE ONLY public.product_piece_code
    ADD CONSTRAINT product_piece_code_code_key UNIQUE (code);

ALTER TABLE ONLY public.product_piece_code
    ADD CONSTRAINT product_piece_code_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_rate
    ADD CONSTRAINT product_rate_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_unit
    ADD CONSTRAINT product_unit_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_unit
    ADD CONSTRAINT product_unit_serial_key UNIQUE (serial);

ALTER TABLE ONLY public.production_entry_detail
    ADD CONSTRAINT production_entry_detail_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.production_entry
    ADD CONSTRAINT production_entry_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sales_order
    ADD CONSTRAINT sales_order_invoice_no_key UNIQUE (invoice_no);

ALTER TABLE ONLY public.sales_order_item
    ADD CONSTRAINT sales_order_item_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sales_order
    ADD CONSTRAINT sales_order_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.system_setting
    ADD CONSTRAINT system_setting_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.system_setting
    ADD CONSTRAINT system_setting_setting_key_key UNIQUE (setting_key);

ALTER TABLE ONLY public.employee
    ADD CONSTRAINT uk_employee_email UNIQUE (email);

ALTER TABLE ONLY public.investment
    ADD CONSTRAINT uk_investment_invoice_number UNIQUE (invoice_number);

ALTER TABLE ONLY public.product_material_cost
    ADD CONSTRAINT uk_product_material_cost UNIQUE (product_id, effective_from);

ALTER TABLE ONLY public.product_rate
    ADD CONSTRAINT uk_product_rate_effective_date UNIQUE (product_id, effective_from);

ALTER TABLE ONLY public.vendor
    ADD CONSTRAINT vendor_pkey PRIMARY KEY (id);

CREATE INDEX idx_employee_code ON public.employee USING btree (employee_code);

CREATE INDEX idx_employee_name ON public.employee USING btree (full_name);

CREATE INDEX idx_inventory_tx_batch ON public.inventory_transaction USING btree (batch_id);

CREATE INDEX idx_inventory_tx_item ON public.inventory_transaction USING btree (sales_order_item_id);

CREATE INDEX idx_inventory_tx_product ON public.inventory_transaction USING btree (product_id);

CREATE INDEX idx_investment_item_product ON public.investment_item USING btree (product_id);

CREATE INDEX idx_investment_item_type ON public.investment_item USING btree (item_type);

CREATE INDEX idx_investment_payment_investment ON public.investment_payment USING btree (investment_id);

CREATE INDEX idx_investment_purchase_date ON public.investment USING btree (purchase_date);

CREATE INDEX idx_investment_type ON public.investment USING btree (investment_type);

CREATE INDEX idx_investment_vendor ON public.investment USING btree (vendor_id);

CREATE INDEX idx_product_material_cost_effective ON public.product_material_cost USING btree (effective_from);

CREATE INDEX idx_product_material_cost_product ON public.product_material_cost USING btree (product_id);

CREATE INDEX idx_product_unit_batch ON public.product_unit USING btree (batch_number);

CREATE INDEX idx_product_unit_status ON public.product_unit USING btree (status);

CREATE INDEX idx_sales_item_order ON public.sales_order_item USING btree (sales_order_id);

CREATE INDEX idx_sales_item_product ON public.sales_order_item USING btree (product_id);

CREATE INDEX idx_sales_order_invoice ON public.sales_order USING btree (invoice_no);

CREATE INDEX idx_sales_order_mobile ON public.sales_order USING btree (customer_mobile);

CREATE INDEX idx_vendor_active ON public.vendor USING btree (active);

CREATE INDEX idx_vendor_created_at ON public.vendor USING btree (created_at);

CREATE UNIQUE INDEX uk_vendor_email ON public.vendor USING btree (email) WHERE ((email IS NOT NULL) AND (is_deleted = false));

CREATE UNIQUE INDEX uk_vendor_gst ON public.vendor USING btree (gst_number) WHERE ((gst_number IS NOT NULL) AND (is_deleted = false));

CREATE UNIQUE INDEX uk_vendor_mobile ON public.vendor USING btree (mobile) WHERE (is_deleted = false);

CREATE UNIQUE INDEX uk_vendor_name ON public.vendor USING btree (lower((name)::text)) WHERE (is_deleted = false);

ALTER TABLE ONLY public.inventory_batch
    ADD CONSTRAINT fk_inventory_batch_product FOREIGN KEY (product_id) REFERENCES public.product(id);

ALTER TABLE ONLY public.inventory_transaction
    ADD CONSTRAINT fk_inventory_transaction_batch FOREIGN KEY (batch_id) REFERENCES public.inventory_batch(id);

ALTER TABLE ONLY public.inventory_transaction
    ADD CONSTRAINT fk_inventory_transaction_item FOREIGN KEY (sales_order_item_id) REFERENCES public.sales_order_item(id);

ALTER TABLE ONLY public.inventory_transaction
    ADD CONSTRAINT fk_inventory_transaction_product FOREIGN KEY (product_id) REFERENCES public.product(id);

ALTER TABLE ONLY public.investment_item
    ADD CONSTRAINT fk_investment_item_investment FOREIGN KEY (investment_id) REFERENCES public.investment(id);

ALTER TABLE ONLY public.investment_item
    ADD CONSTRAINT fk_investment_item_product FOREIGN KEY (product_id) REFERENCES public.product(id);

ALTER TABLE ONLY public.investment_payment
    ADD CONSTRAINT fk_investment_payment_investment FOREIGN KEY (investment_id) REFERENCES public.investment(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.investment
    ADD CONSTRAINT fk_investment_vendor FOREIGN KEY (vendor_id) REFERENCES public.vendor(id);

ALTER TABLE ONLY public.product_piece_code
    ADD CONSTRAINT fk_piece_code_product FOREIGN KEY (product_id) REFERENCES public.product(id);

ALTER TABLE ONLY public.product_material_cost
    ADD CONSTRAINT fk_product_material_cost_product FOREIGN KEY (product_id) REFERENCES public.product(id);

ALTER TABLE ONLY public.product_rate
    ADD CONSTRAINT fk_product_rate_product FOREIGN KEY (product_id) REFERENCES public.product(id);

ALTER TABLE ONLY public.product_unit
    ADD CONSTRAINT fk_product_unit_product FOREIGN KEY (product_id) REFERENCES public.product(id);

ALTER TABLE ONLY public.production_entry_detail
    ADD CONSTRAINT fk_production_detail_entry FOREIGN KEY (production_entry_id) REFERENCES public.production_entry(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.production_entry_detail
    ADD CONSTRAINT fk_production_detail_product FOREIGN KEY (product_id) REFERENCES public.product(id);

ALTER TABLE ONLY public.production_entry
    ADD CONSTRAINT fk_production_employee FOREIGN KEY (employee_id) REFERENCES public.employee(id);

ALTER TABLE ONLY public.production_entry_detail
    ADD CONSTRAINT fk_production_piece_code FOREIGN KEY (piece_code_id) REFERENCES public.product_piece_code(id);

ALTER TABLE ONLY public.sales_order_item
    ADD CONSTRAINT fk_sales_item_order FOREIGN KEY (sales_order_id) REFERENCES public.sales_order(id);

ALTER TABLE ONLY public.sales_order_item
    ADD CONSTRAINT fk_sales_item_product FOREIGN KEY (product_id) REFERENCES public.product(id);

ALTER TABLE ONLY public.product_image
    ADD CONSTRAINT product_image_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.product(id) ON DELETE CASCADE;


