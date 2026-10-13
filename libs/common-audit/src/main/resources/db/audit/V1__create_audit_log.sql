-- Append-only, hash-chained audit log (ADR 0008). Applied by AuditSchemaMigrator into the "audit" schema
-- with its own history table, so it never collides with a service's own migration versions.

CREATE TABLE audit_log (
    id            uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    sequence_no   bigint       NOT NULL UNIQUE CHECK (sequence_no > 0),
    occurred_at   timestamptz  NOT NULL,
    actor_id      varchar(128) NOT NULL,
    action        varchar(64)  NOT NULL,
    resource_type varchar(64)  NOT NULL,
    resource_id   varchar(128) NOT NULL,
    payload       text         NOT NULL,
    previous_hash char(64)     NOT NULL,
    hash          char(64)     NOT NULL UNIQUE,
    created_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX audit_log_resource_idx ON audit_log (resource_type, resource_id);
CREATE INDEX audit_log_actor_idx ON audit_log (actor_id);
CREATE INDEX audit_log_occurred_at_idx ON audit_log (occurred_at);

-- One row holding the end of the chain; appenders lock it, which orders concurrent writes.
CREATE TABLE audit_chain_head (
    id               smallint    PRIMARY KEY CHECK (id = 1),
    last_sequence_no bigint      NOT NULL,
    last_hash        char(64)    NOT NULL,
    updated_at       timestamptz NOT NULL DEFAULT now()
);

INSERT INTO audit_chain_head (id, last_sequence_no, last_hash) VALUES (1, 0, repeat('0', 64));

CREATE FUNCTION reject_audit_log_change() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only: % is not allowed', TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER audit_log_append_only
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION reject_audit_log_change();

CREATE TRIGGER audit_log_no_truncate
    BEFORE TRUNCATE ON audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION reject_audit_log_change();
