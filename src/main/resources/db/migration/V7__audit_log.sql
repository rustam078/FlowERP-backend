-- Audit trail of important write actions (money, stock, payroll, security). Kept 60 days.
-- No foreign keys to portfolio / user on purpose: the history must survive those rows changing.

-- One row per audited request: who, when, where, what was sent, what came back.
CREATE TABLE audit_log (
    id uuid NOT NULL,
    request_id uuid NOT NULL,
    portfolio_id uuid,
    user_id uuid,
    user_email varchar(150),
    user_role varchar(50),
    action varchar(150) NOT NULL,
    module varchar(40),
    http_method varchar(10) NOT NULL,
    url_path varchar(500) NOT NULL,
    url_pattern varchar(300),
    query_params jsonb,
    request_body jsonb,
    response_status integer NOT NULL,
    response_body jsonb,
    error_message text,
    duration_ms integer NOT NULL,
    client_ip varchar(45),
    user_agent varchar(300),
    created_at timestamp NOT NULL,
    CONSTRAINT audit_log_pkey PRIMARY KEY (id),
    CONSTRAINT uk_audit_log_request UNIQUE (request_id)
);
CREATE INDEX idx_audit_log_created ON audit_log (created_at);
CREATE INDEX idx_audit_log_portfolio_created ON audit_log (portfolio_id, created_at);
CREATE INDEX idx_audit_log_user_created ON audit_log (user_id, created_at);

-- One row per database row the request changed: exact state before and after.
CREATE TABLE audit_log_change (
    id uuid NOT NULL,
    audit_log_id uuid NOT NULL,
    portfolio_id uuid,
    entity_name varchar(80) NOT NULL,
    entity_id uuid,
    operation varchar(10) NOT NULL,
    before_state jsonb,
    after_state jsonb,
    changed_fields jsonb,
    created_at timestamp NOT NULL,
    CONSTRAINT audit_log_change_pkey PRIMARY KEY (id),
    CONSTRAINT fk_audit_log_change_log FOREIGN KEY (audit_log_id) REFERENCES audit_log (id) ON DELETE CASCADE,
    CONSTRAINT chk_audit_log_change_operation CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE'))
);
CREATE INDEX idx_audit_log_change_log ON audit_log_change (audit_log_id);
CREATE INDEX idx_audit_log_change_entity ON audit_log_change (entity_name, entity_id);
