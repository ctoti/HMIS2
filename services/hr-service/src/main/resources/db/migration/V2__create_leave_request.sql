CREATE TABLE leave_request (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id            uuid        NOT NULL REFERENCES staff (id),
    -- Copied from the staff record at submission, so a later transfer does not move past requests.
    facility_code       varchar(32) NOT NULL,
    leave_type          varchar(32) NOT NULL,
    start_date          date        NOT NULL,
    end_date            date        NOT NULL,
    status              varchar(16) NOT NULL,
    requested_by        varchar(64) NOT NULL,
    idempotency_key     varchar(100) NOT NULL,
    request_fingerprint char(64)    NOT NULL,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    version             bigint      NOT NULL DEFAULT 0,
    CONSTRAINT leave_request_dates_check CHECK (end_date >= start_date),
    CONSTRAINT leave_request_type_check CHECK (leave_type IN
        ('ANNUAL', 'SICK', 'FAMILY', 'STUDY', 'MATERNITY', 'PATERNITY', 'LEAVE_WITHOUT_PAY')),
    CONSTRAINT leave_request_status_check CHECK (status IN ('SUBMITTED', 'CANCELLED')),
    CONSTRAINT leave_request_idempotency_key UNIQUE (requested_by, idempotency_key)
);

CREATE INDEX leave_request_staff_id_idx ON leave_request (staff_id, start_date);
CREATE INDEX leave_request_facility_code_idx ON leave_request (facility_code, status);
