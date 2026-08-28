CREATE TABLE email_outbox
(
    event_id       UUID         NOT NULL,
    crime_batch_id UUID,
    status         VARCHAR(255) NOT NULL,
    payload        JSONB        NOT NULL,
    attempts       INTEGER      NOT NULL DEFAULT 0,
    available_at   TIMESTAMP    NOT NULL,
    claimed_at     TIMESTAMP,
    claimed_by     VARCHAR(255),
    last_error     TEXT,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    CONSTRAINT pk_email_outbox PRIMARY KEY (event_id)
);

-- Supports the relay claim query: WHERE status = 'PENDING' AND available_at <= now() ORDER BY created_at
CREATE INDEX idx_email_outbox_status_available_at ON email_outbox (status, available_at, created_at);

