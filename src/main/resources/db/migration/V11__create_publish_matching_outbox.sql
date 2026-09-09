CREATE TABLE publish_matching_outbox
(
    id               UUID NOT NULL,
    payload TEXT,
    state       VARCHAR(30) NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_publish_matching_outbox PRIMARY KEY (id)
);
