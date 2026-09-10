CREATE TABLE publish_matching_outbox
(
    id               UUID NOT NULL,
    payload TEXT,
    state       VARCHAR(30) NOT NULL,
    created_at        BIGINT NOT NULL,
    claimed_at        BIGINT,
    attempts INTEGER NOT NULL,
    last_error TEXT,
    CONSTRAINT pk_publish_matching_outbox PRIMARY KEY (id)
);
