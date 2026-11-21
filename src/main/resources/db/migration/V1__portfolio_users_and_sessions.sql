-- Portfolios (tenants), users (email login; SUPER_ADMIN has no portfolio) and server-side login sessions.

-- batch_prefix: 2 capital letters chosen at creation, unique and never changed (batch numbers IL001, IL002 ...).
-- batch_seq / sale_invoice_seq / purchase_invoice_seq: last number used by this portfolio (taken atomically).
CREATE TABLE portfolio (
    id uuid NOT NULL,
    name varchar(120) NOT NULL,
    batch_prefix varchar(2) NOT NULL,
    batch_seq bigint DEFAULT 0 NOT NULL,
    sale_invoice_seq bigint DEFAULT 0 NOT NULL,
    purchase_invoice_seq bigint DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT portfolio_pkey PRIMARY KEY (id),
    CONSTRAINT uk_portfolio_batch_prefix UNIQUE (batch_prefix),
    CONSTRAINT chk_portfolio_batch_prefix CHECK (batch_prefix ~ '^[A-Z]{2}$')
);

CREATE TABLE app_user (
    id uuid NOT NULL,
    portfolio_id uuid,
    password varchar(255) NOT NULL,
    full_name varchar(150) NOT NULL,
    role varchar(50) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    email varchar(150) NOT NULL,
    CONSTRAINT app_user_pkey PRIMARY KEY (id),
    CONSTRAINT chk_app_user_portfolio CHECK (((((role)::text = 'SUPER_ADMIN'::text) AND (portfolio_id IS NULL)) OR (((role)::text <> 'SUPER_ADMIN'::text) AND (portfolio_id IS NOT NULL)))),
    CONSTRAINT app_user_portfolio_id_fkey FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);
CREATE INDEX idx_app_user_portfolio ON app_user USING btree (portfolio_id);
CREATE UNIQUE INDEX uk_app_user_email ON app_user USING btree (lower((email)::text));

CREATE TABLE user_session (
    id uuid NOT NULL,
    token_hash varchar(64) NOT NULL,
    user_id uuid NOT NULL,
    active_portfolio_id uuid,
    last_seen_at timestamp NOT NULL,
    expires_at timestamp NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL,
    CONSTRAINT user_session_pkey PRIMARY KEY (id),
    CONSTRAINT user_session_token_hash_key UNIQUE (token_hash),
    CONSTRAINT user_session_active_portfolio_id_fkey FOREIGN KEY (active_portfolio_id) REFERENCES portfolio(id),
    CONSTRAINT user_session_user_id_fkey FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);
CREATE INDEX idx_user_session_user ON user_session USING btree (user_id);

-- Default portfolio.
INSERT INTO portfolio (id, name, batch_prefix, batch_seq, is_active, is_deleted, created_at, updated_at) VALUES ('00000000-0000-0000-0000-000000000001', 'ILITY', 'IL', 0, true, false, now(), now());

-- ILITY portfolio admin and the super admin (passwords stored as BCrypt hashes only).
INSERT INTO app_user (id, portfolio_id, email, password, full_name, role, is_active, created_at, updated_at) VALUES ('3235e0e2-87e9-4625-b614-8f65108f86e6', NULL, 'superadmin@mailinator.com', '$2a$10$4RjmpCBdl.Oz8pvz1pghBuGcl8llF/D1d08VV3.lgVlvkc6me6uni', 'Super Admin', 'SUPER_ADMIN', true, now(), now());
INSERT INTO app_user (id, portfolio_id, email, password, full_name, role, is_active, created_at, updated_at) VALUES ('aee223cc-d162-4e48-98cc-6b47350ba02f', '00000000-0000-0000-0000-000000000001', 'ilityadmin@mailinator.com', '$2a$10$3DJZDhzX2iMmYfTIockdb.Pt5FHEqLKkbOwPTMywIWpsY.yvNQXv2', 'System Administrator', 'ADMIN', true, now(), now());
