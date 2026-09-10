CREATE TABLE publish_matching_outbox
(
    id            UUID NOT NULL,
    payload       TEXT NOT NULL,
    state         VARCHAR(30) NOT NULL,
    created_at    BIGINT NOT NULL,
    claimed_at    BIGINT,
    attempts      INTEGER NOT NULL,
    last_error    TEXT,
    CONSTRAINT pk_publish_matching_outbox PRIMARY KEY (id)
);

CREATE INDEX idx_publish_matching_outbox_state_created_at
    ON publish_matching_outbox (state, created_at);
