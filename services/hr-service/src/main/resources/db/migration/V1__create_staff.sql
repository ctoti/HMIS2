CREATE TABLE staff (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_number varchar(32)  NOT NULL,
    -- Keycloak subject of the staff member's own account; null until an account is linked.
    user_id         varchar(64),
    given_name      varchar(100) NOT NULL,
    family_name     varchar(100) NOT NULL,
    facility_code   varchar(32)  NOT NULL,
    position_title  varchar(120) NOT NULL,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    version         bigint       NOT NULL DEFAULT 0,
    CONSTRAINT staff_employee_number_key UNIQUE (employee_number),
    CONSTRAINT staff_user_id_key UNIQUE (user_id)
);

CREATE INDEX staff_facility_code_idx ON staff (facility_code);
