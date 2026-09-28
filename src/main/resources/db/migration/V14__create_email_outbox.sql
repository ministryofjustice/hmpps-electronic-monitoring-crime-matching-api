CREATE TABLE email_outbox
(
    id            UUID NOT NULL,
    payload       TEXT NOT NULL,
    state         VARCHAR(30) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    claimed_at    TIMESTAMPTZ,
    attempts      INTEGER NOT NULL,
    last_error    TEXT,
    version       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_email_outbox PRIMARY KEY (id)
);

CREATE INDEX idx_email_outbox_state_created_at
    ON email_outbox (state, created_at);
