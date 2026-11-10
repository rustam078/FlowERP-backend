

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





INSERT INTO public.app_user (id, username, password, full_name, role, is_active, created_at, updated_at) VALUES ('aee223cc-d162-4e48-98cc-6b47350ba02f', 'admin', 'admin123', 'System Administrator', 'ADMIN', true, '2026-09-28 09:32:26.399994', '2026-09-28 09:32:26.399994');

INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('90de9576-25db-40a2-859e-cb8db67aac72', 'LOW_STOCK_THRESHOLD', '50', 'Global minimum stock alert threshold', '2026-09-28 09:32:26.747143', '2026-09-28 09:32:26.747143');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('88ee4f90-c078-48ba-9d4d-793d01a5d5dd', 'QR_LABEL_CONFIG', '{"showName":true,"showPrice":true,"showSerial":true,"nameVertical":false}', 'QR label print settings: show/hide fields and product-name orientation', '2026-09-28 09:32:26.846862', '2026-09-28 09:32:26.846862');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('bc1b9621-13e6-47de-ba08-08dc613a272f', 'COMPANY_NAME', 'SKR Garment', 'Company name shown on invoices and headers', '2026-09-28 09:32:26.870405', '2026-09-28 09:32:26.870405');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('6d03d7d2-f735-4a96-bbbd-1718fb217221', 'COMPANY_ADDRESS', '14 Loom Street, Industrial Area, Patna, Bihar 800001', 'Company address shown on invoices', '2026-09-28 09:32:26.870405', '2026-09-28 09:32:26.870405');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('cf220e2a-27b6-4e8e-8033-5f846ae58b6b', 'COMPANY_CONTACT', '+91 90000 12345', 'Company phone / email shown on invoices', '2026-09-28 09:32:26.870405', '2026-09-28 09:32:26.870405');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('5e11a702-f8d0-4328-b137-85a191279cb2', 'COMPANY_GSTIN', '10ABCDE1234F1Z5', 'Company GST identification number', '2026-09-28 09:32:26.870405', '2026-09-28 09:32:26.870405');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('a5ac9cd5-50cf-4657-9828-7c5b832fc968', 'DEFAULT_GST_PERCENT', '5', 'Default GST percent applied to new invoices (snapshotted per invoice)', '2026-09-28 09:32:26.870405', '2026-09-28 09:32:26.870405');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('aeef3a15-08de-466b-bf45-79a5d23c81f8', 'CURRENCY_SYMBOL', '₹', 'Currency symbol used across the app', '2026-09-28 09:32:26.870405', '2026-09-28 09:32:26.870405');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('794c4226-76b1-4fa3-a7c5-0bebb1b50aaf', 'PURCHASE_INVOICE_TEMPLATE', '<html>
<head>
<style>
  @page { size: A4; margin: 34px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #1a1a1a; font-size: 12px; }
  .head { width: 100%; border-collapse: collapse; }
  .head td { vertical-align: top; }
  .cname { font-size: 20px; font-weight: bold; letter-spacing: 0.5px; }
  .csub { font-size: 9px; color: #555555; letter-spacing: 1px; margin-top: 2px; }
  .caddr { font-size: 10px; color: #444444; margin-top: 6px; line-height: 1.5; }
  .doc-title { font-size: 22px; font-weight: bold; text-align: right; letter-spacing: 3px; }
  .doc-sub { font-size: 10px; color: #555555; text-align: right; letter-spacing: 3px; margin-top: 2px; }
  .rule { border-bottom: 2px solid #000000; margin: 8px 0 14px 0; }
  .parties { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
  .parties td { vertical-align: top; width: 50%; padding-right: 18px; }
  .eyebrow { font-size: 9px; font-weight: bold; color: #888888; letter-spacing: 1px; margin-bottom: 5px; }
  .pname { font-size: 13px; font-weight: bold; }
  .pline { font-size: 11px; color: #444444; margin-top: 2px; }
  .meta-t { width: 100%; border-collapse: collapse; }
  .meta-t td { padding: 3px 0; font-size: 11px; }
  .meta-k { color: #666666; }
  .meta-v { font-weight: bold; text-align: right; }
  table.items { width: 100%; border-collapse: collapse; margin: 4px 0 12px 0; }
  table.items th { text-align: left; padding: 8px; font-size: 10px; letter-spacing: 0.5px; border-top: 1.5px solid #000000; border-bottom: 1.5px solid #000000; background-color: #f2f2f0; }
  table.items td { padding: 9px 8px; border-bottom: 1px solid #dddddd; font-size: 11px; vertical-align: top; }
  .right { text-align: right; }
  .itype { font-size: 9px; color: #666666; margin-top: 2px; }
  .totals { width: 46%; margin-left: 54%; border-collapse: collapse; margin-bottom: 4px; }
  .totals td { padding: 5px 8px; font-size: 11px; }
  .totals .k { color: #555555; }
  .totals .grand td { border-top: 2px solid #000000; font-size: 15px; font-weight: bold; padding-top: 9px; }
  .pay-h { font-size: 10px; font-weight: bold; letter-spacing: 1px; border-bottom: 1.5px solid #000000; padding-bottom: 5px; margin-top: 14px; }
  .pay-sum { width: 100%; border-collapse: collapse; margin: 10px 0; }
  .pay-sum td { font-size: 11px; padding: 2px 0; }
  table.ph { width: 100%; border-collapse: collapse; }
  table.ph th { text-align: left; font-size: 9px; color: #666666; letter-spacing: 0.5px; padding: 5px 8px; border-bottom: 1px solid #cccccc; }
  table.ph td { font-size: 11px; padding: 7px 8px; border-bottom: 1px solid #eeeeee; }
  .end { width: 100%; border-collapse: collapse; margin-top: 28px; }
  .end td { vertical-align: bottom; }
  .terms { font-size: 10px; color: #555555; width: 62%; line-height: 1.6; }
  .sign { font-size: 10px; color: #555555; text-align: center; }
  .sline { border-top: 1px solid #000000; padding-top: 4px; margin-top: 40px; }
  .foot { margin-top: 18px; border-top: 1px solid #dddddd; padding-top: 8px; text-align: center; color: #999999; font-size: 9px; }
</style>
</head>
<body>
  <table class="head">
    <tr>
      <td>
        <div class="cname">{{companyName}}</div>
        <div class="csub">MANUFACTURING AND TRADING</div>
        <div class="caddr">{{companyAddress}}</div>
        <div class="caddr">{{companyContact}} &#160;&#183;&#160; GSTIN {{companyGstin}}</div>
      </td>
      <td>
        <div class="doc-title">INVOICE</div>
        <div class="doc-sub">PURCHASE</div>
      </td>
    </tr>
  </table>
  <div class="rule"></div>

  <table class="parties">
    <tr>
      <td>
        <div class="eyebrow">VENDOR</div>
        <div class="pname">{{vendorName}}</div>
        <div class="pline">{{vendorAddress}}</div>
        <div class="pline">{{vendorContact}}</div>
        <div class="pline">GSTIN {{vendorGstin}}</div>
      </td>
      <td>
        <table class="meta-t">
          <tr><td class="meta-k">Invoice No.</td><td class="meta-v">{{invoiceNumber}}</td></tr>
          <tr><td class="meta-k">Invoice Date</td><td class="meta-v">{{invoiceDate}}</td></tr>
          <tr><td class="meta-k">Type</td><td class="meta-v">{{invoiceType}}</td></tr>
          <tr><td class="meta-k">Status</td><td class="meta-v">{{paymentStatus}}</td></tr>
        </table>
      </td>
    </tr>
  </table>

  <table class="items">
    <thead>
      <tr>
        <th style="width: 26px;">#</th>
        <th>DESCRIPTION</th>
        <th class="right" style="width: 70px;">QTY</th>
        <th style="width: 58px;">UNIT</th>
        <th class="right" style="width: 82px;">RATE</th>
        <th class="right" style="width: 96px;">AMOUNT</th>
      </tr>
    </thead>
    <tbody>
      {{itemRows}}
    </tbody>
  </table>

  <table class="totals">
    <tr><td class="k">Subtotal</td><td class="right">{{subtotal}}</td></tr>
    <tr><td class="k">GST</td><td class="right">{{gst}}</td></tr>
    <tr><td class="k">Discount</td><td class="right">- {{discount}}</td></tr>
    <tr><td class="k">Other Charges</td><td class="right">{{otherCharge}}</td></tr>
    <tr class="grand"><td>Grand Total</td><td class="right">{{grandTotal}}</td></tr>
  </table>

  <div class="pay-h">PAYMENT DETAILS</div>
  <table class="pay-sum">
    <tr>
      <td>Amount Paid: <b>{{amountPaid}}</b></td>
      <td>Balance Due: <b>{{amountDue}}</b></td>
      <td>Status: <b>{{paymentStatus}}</b></td>
    </tr>
  </table>
  <table class="ph">
    <thead>
      <tr><th style="width: 130px;">DATE</th><th>MODE</th><th class="right" style="width: 110px;">AMOUNT</th></tr>
    </thead>
    <tbody>
      {{paymentRows}}
    </tbody>
  </table>

  <table class="end">
    <tr>
      <td class="terms"><b>Terms.</b> Goods received in full and inspected. Balance payable within 30 days of the invoice date. Please quote the invoice number on all payments.</td>
      <td class="sign"><div>For {{companyName}}</div><div class="sline">Authorised Signatory</div></td>
    </tr>
  </table>

  <div class="foot">Generated on {{generatedOn}}</div>
</body>
</html>', 'HTML template for the purchase (vendor) invoice PDF (placeholders filled at runtime)', '2026-09-28 09:32:26.882718', '2026-09-28 09:32:26.882718');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('5675c047-9b10-40dc-8894-e32504ac4003', 'MENU_ORDER', '["dashboard","production","inventory","products","investment","employees","sales","settings"]', 'Order of the left sidebar menu items (JSON array of keys, ascending)', '2026-09-28 09:32:26.89465', '2026-09-28 09:32:26.89465');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('44f481d4-be41-49e5-ab06-e4f6f5a9e6e9', 'SALES_GST_ENABLED', 'false', 'When true, GST (DEFAULT_GST_PERCENT) is added to every new sale; when false, sales have no GST', '2026-09-28 09:32:26.909267', '2026-09-28 09:32:26.909267');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('7407a6aa-a7d0-4b6d-ab7b-b4850e053f78', 'SALES_INVOICE_TEMPLATE', '<html>
<head>
<style>
  @page { size: A4; margin: 32px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #000; font-size: 12px; }
  .head { width: 100%; border-collapse: collapse; margin-bottom: 8px; }
  .head td { vertical-align: top; }
  .brand { font-size: 24px; font-weight: bold; color: #000; letter-spacing: 1px; }
  .inv-title { font-size: 26px; font-weight: bold; color: #000; text-align: right; }
  .inv-no { font-size: 12px; color: #555; text-align: right; }
  .divider { border-bottom: 3px solid #000; margin: 6px 0 16px 0; }
  .meta { width: 100%; border-collapse: collapse; margin-bottom: 18px; }
  .meta td { vertical-align: top; padding: 0 6px; width: 50%; }
  .label { font-size: 10px; font-weight: bold; color: #555; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 3px; }
  .value { font-size: 13px; font-weight: bold; color: #000; }
  .muted { color: #555; font-size: 11px; margin-top: 2px; }
  .badge { display: inline-block; padding: 3px 10px; border: 1px solid #000; border-radius: 10px; font-size: 10px; font-weight: bold; color: #000; }
  .badge-paid { background-color: #ffffff; }
  .badge-other { background-color: #dcdcdc; }
  table.items { width: 100%; border-collapse: collapse; margin-bottom: 14px; table-layout: fixed; }
  table.items th { background-color: #ececec; color: #000; text-align: left; padding: 8px 10px; font-size: 11px; border: 1px solid #000; }
  table.items td { padding: 8px 10px; border-bottom: 1px solid #999; word-wrap: break-word; }
  .right { text-align: right; }
  .totals { width: 46%; margin-left: 54%; border-collapse: collapse; }
  .totals td { padding: 5px 10px; }
  .totals .grand td { border-top: 2px solid #000; font-size: 15px; font-weight: bold; color: #000; }
  .footer { margin-top: 30px; border-top: 1px solid #999; padding-top: 10px; color: #555; font-size: 10px; text-align: center; }
</style>
</head>
<body>
  <table class="head">
    <tr>
      <td>
        <div class="brand">{{companyName}}</div>
        <div class="muted">{{companyAddress}}</div>
        <div class="muted">{{companyContact}} &#160;&#183;&#160; GSTIN {{companyGstin}}</div>
      </td>
      <td>
        <div class="inv-title">INVOICE</div>
        <div class="inv-no">{{invoiceNumber}}</div>
      </td>
    </tr>
  </table>
  <div class="divider"></div>

  <table class="meta">
    <tr>
      <td>
        <div class="label">Billed To</div>
        <div class="value">{{customerName}}</div>
        <div class="muted">{{customerMobile}}</div>
        <div class="muted">{{customerEmail}}</div>
      </td>
      <td>
        <div class="label">Invoice Date</div>
        <div class="value">{{date}}</div>
        <div class="label" style="margin-top:8px;">Payment</div>
        <div class="value">{{paymentMode}} <span class="badge {{statusClass}}">{{paymentStatus}}</span></div>
        <div class="muted">{{paymentProvider}}</div>
      </td>
    </tr>
  </table>

  <table class="items">
    <thead>
      <tr>
        <th>Product</th>
        <th class="right" style="width: 62px;">Qty</th>
        <th class="right" style="width: 92px;">Price</th>
        <th class="right" style="width: 92px;">Discount</th>
        <th class="right" style="width: 104px;">Amount</th>
      </tr>
    </thead>
    <tbody>
      {{itemRows}}
    </tbody>
  </table>

  <table class="totals">
    <tr><td>Subtotal</td><td class="right">{{cur}} {{subtotal}}</td></tr>
    <tr><td>Discount</td><td class="right">- {{cur}} {{discount}}</td></tr>
    <tr><td>Tax</td><td class="right">{{cur}} {{tax}}</td></tr>
    <tr class="grand"><td>Grand Total</td><td class="right">{{cur}} {{grandTotal}}</td></tr>
    {{paymentRows}}
  </table>

  <div class="footer">Thank you for your business. Generated on {{generatedOn}}.</div>
</body>
</html>', 'HTML template for sales invoice PDF (placeholders filled at runtime)', '2026-09-28 09:32:26.776259', '2026-09-28 09:32:26.776259');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('a0c516df-ffd4-4aca-b0a4-d57d1d5b8cfb', 'PRODUCTION_PDF_TEMPLATE', '<html>
<head>
<style>
  @page { size: A4; margin: 26px; }
  body { font-family: Helvetica, Arial, sans-serif; color: #1E293B; font-size: 12px; }
  .header { text-align: center; border-bottom: 3px solid #2563EB; padding-bottom: 10px; margin-bottom: 16px; }
  .brand { font-size: 22px; font-weight: bold; color: #2563EB; letter-spacing: 1px; }
  .subtitle { font-size: 11px; color: #64748B; margin-top: 2px; }
  .report-title { font-size: 15px; font-weight: bold; margin-top: 8px; color: #0F172A; }
  .ref { color: #64748B; font-size: 11px; margin-bottom: 8px; }
  .info-table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
  .info-table td { padding: 5px 6px; vertical-align: top; }
  .info-label { color: #64748B; font-weight: bold; width: 130px; }
  .items { width: 100%; border-collapse: collapse; margin-bottom: 14px; table-layout: fixed; }
  .items th { background-color: #F1F5F9; color: #334155; text-align: left; padding: 7px 8px; border: 1px solid #E2E8F0; font-size: 11px; }
  .items td { padding: 7px 8px; border: 1px solid #E2E8F0; word-wrap: break-word; }
  .right { text-align: right; }
  .summary { width: 45%; margin-left: 55%; border-collapse: collapse; margin-top: 4px; }
  .summary td { padding: 5px 8px; border-bottom: 1px solid #E2E8F0; }
  .summary .tot { font-weight: bold; color: #047857; font-size: 13px; }
  .footer { margin-top: 26px; text-align: right; color: #94A3B8; font-size: 10px; border-top: 1px solid #E2E8F0; padding-top: 8px; }
</style>
</head>
<body>
  <div class="header">
    <div class="brand">{{companyName}}</div>
    <div class="subtitle">{{companyAddress}}</div>
    <div class="subtitle">{{companyContact}} &#160;&#183;&#160; GSTIN {{companyGstin}}</div>
    <div class="report-title">Production Report</div>
  </div>
  <div class="ref">Ref ID: {{refId}}</div>
  <table class="info-table">
    <tr><td class="info-label">Production Date</td><td>{{productionDate}}</td></tr>
    <tr><td class="info-label">Employee</td><td>{{employeeName}}</td></tr>
    <tr><td class="info-label">Remarks</td><td>{{remarks}}</td></tr>
  </table>
  <table class="items">
    <thead>
      <tr>
        <th>Product</th>
        <th style="width: 110px;">Piece Code</th>
        <th class="right" style="width: 80px;">Quantity</th>
        <th class="right" style="width: 90px;">Rate</th>
        <th class="right" style="width: 100px;">Amount</th>
      </tr>
    </thead>
    <tbody>
      {{itemRows}}
    </tbody>
  </table>
  <table class="summary">
    <tr><td>Total Products</td><td class="right">{{productCount}}</td></tr>
    <tr><td>Total Quantity</td><td class="right">{{totalQuantity}} Pcs</td></tr>
    <tr><td class="tot">Total Amount</td><td class="right tot">Rs. {{totalAmount}}</td></tr>
  </table>
  <div class="footer">Generated on {{generatedOn}}</div>
</body>
</html>', 'HTML template for production PDF export (placeholders filled at runtime)', '2026-09-28 09:32:26.765098', '2026-09-28 09:32:26.765098');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('d1767e99-9202-4d62-b8a7-e64da7abda9f', 'ROLE_PERMISSIONS', '{"STAFF":{"dashboard":{"view":true,"write":false,"delete":false},"sales":{"view":true,"write":true,"delete":false},"inventory":{"view":true,"write":false,"delete":false},"products":{"view":true,"write":false,"delete":false},"investment":{"view":true,"write":false,"delete":false},"production":{"view":false,"write":false,"delete":false},"employees":{"view":false,"write":false,"delete":false},"settings":{"view":false,"write":false,"delete":false}}}', 'Per-role, per-module permissions (view/write/delete) for STAFF; ADMIN is always full access', '2026-09-28 09:32:26.931597', '2026-09-28 09:32:26.931597');
INSERT INTO public.system_setting (id, setting_key, setting_value, description, created_at, updated_at) VALUES ('2fa87584-6e37-4d50-8b76-369cddfeca16', 'STAFF_DASHBOARD_BOARDS', '["sales","inventory"]', 'Dashboard boards visible to STAFF (JSON array of board keys); ADMIN sees all', '2026-09-28 09:32:26.940362', '2026-09-28 09:32:26.940362');


