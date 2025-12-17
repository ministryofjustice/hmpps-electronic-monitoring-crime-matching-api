CREATE TABLE crime
(
    id                UUID         NOT NULL,
    police_force_area VARCHAR(255) NOT NULL,
    crime_reference   VARCHAR(255) NOT NULL,
    CONSTRAINT pk_crime PRIMARY KEY (id)
);

CREATE TABLE crime_batch
(
    id                              UUID         NOT NULL,
    batch_id                        VARCHAR(255) NOT NULL,
    crime_batch_email_attachment_id UUID         NOT NULL,
    created_at                      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_crime_batch PRIMARY KEY (id)
);

CREATE TABLE crime_batch_crime_version
(
    crime_batch_id   UUID NOT NULL,
    crime_version_id UUID NOT NULL,
    CONSTRAINT pk_crime_batch_crime_version PRIMARY KEY (crime_batch_id, crime_version_id)
);

CREATE TABLE crime_batch_email
(
    id                               UUID NOT NULL,
    crime_batch_ingestion_attempt_id UUID NOT NULL,
    sender                           VARCHAR(255),
    original_sender                  VARCHAR(255),
    subject                          VARCHAR(255),
    sent_at                          TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_crime_batch_email PRIMARY KEY (id)
);

CREATE TABLE crime_batch_email_attachment
(
    id                   UUID    NOT NULL,
    crime_batch_email_id UUID    NOT NULL,
    file_name            VARCHAR(255),
    row_count            INTEGER NOT NULL,
    CONSTRAINT pk_crime_batch_email_attachment PRIMARY KEY (id)
);

CREATE TABLE crime_batch_email_attachment_ingestion_error
(
    id                              UUID    NOT NULL,
    crime_batch_email_attachment_id UUID    NOT NULL,
    row_number                      INTEGER NOT NULL,
    crime_reference                 VARCHAR(255),
    error_type                      VARCHAR(255),
    CONSTRAINT pk_crime_batch_email_attachment_ingestion_error PRIMARY KEY (id)
);

CREATE TABLE crime_batch_ingestion_attempt
(
    id          UUID NOT NULL,
    bucket      VARCHAR(255),
    object_name VARCHAR(255),
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_crime_batch_ingestion_attempt PRIMARY KEY (id)
);

CREATE TABLE crime_version
(
    id                   UUID NOT NULL,
    crime_id             UUID NOT NULL,
    crime_type_id        VARCHAR(255) NOT NULL,
    crime_date_time_from TIMESTAMP WITHOUT TIME ZONE,
    crime_date_time_to   TIMESTAMP WITHOUT TIME ZONE,
    easting              DOUBLE PRECISION,
    northing             DOUBLE PRECISION,
    latitude             DOUBLE PRECISION,
    longitude            DOUBLE PRECISION,
    crime_text           VARCHAR(255),
    created_at           TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_crime_version PRIMARY KEY (id)
);

ALTER TABLE crime
    ADD CONSTRAINT uc_crime_reference_police_force_area UNIQUE (crime_reference, police_force_area);

ALTER TABLE crime_batch_crime_version
    ADD CONSTRAINT uc_crime_batch_id_crime_version_id UNIQUE (crime_batch_id, crime_version_id);

ALTER TABLE crime_batch
    ADD CONSTRAINT uc_crime_batch_crime_batch_email_attachment UNIQUE (crime_batch_email_attachment_id);

ALTER TABLE crime_batch_email_attachment
    ADD CONSTRAINT uc_crime_batch_email_attachment_crime_batch_email UNIQUE (crime_batch_email_id);

ALTER TABLE crime_batch_email
    ADD CONSTRAINT uc_crime_batch_email_crime_batch_ingestion_attempt UNIQUE (crime_batch_ingestion_attempt_id);

ALTER TABLE crime_batch_email_attachment
    ADD CONSTRAINT FK_CRIME_BATCH_EMAIL_ATTACHMENT_ON_CRIME_BATCH_EMAIL FOREIGN KEY (crime_batch_email_id) REFERENCES crime_batch_email (id);

ALTER TABLE crime_batch_email
    ADD CONSTRAINT FK_CRIME_BATCH_EMAIL_ON_CRIME_BATCH_INGESTION_ATTEMPT FOREIGN KEY (crime_batch_ingestion_attempt_id) REFERENCES crime_batch_ingestion_attempt (id);

ALTER TABLE crime_batch
    ADD CONSTRAINT FK_CRIME_BATCH_ON_CRIME_BATCH_EMAIL_ATTACHMENT FOREIGN KEY (crime_batch_email_attachment_id) REFERENCES crime_batch_email_attachment (id);

ALTER TABLE crime_batch_email_attachment_ingestion_error
    ADD CONSTRAINT FK_CRIME_BATCH_INGESTION_ERROR_ON_CRIME_BATCH_EMAIL_ATTACHMENT FOREIGN KEY (crime_batch_email_attachment_id) REFERENCES crime_batch_email_attachment (id);

ALTER TABLE crime_version
    ADD CONSTRAINT FK_CRIME_VERSION_ON_CRIME FOREIGN KEY (crime_id) REFERENCES crime (id)
        ON DELETE CASCADE;

ALTER TABLE crime_batch_crime_version
    ADD CONSTRAINT fk_cribatcriver_on_crime_batch FOREIGN KEY (crime_batch_id) REFERENCES crime_batch (id)
        ON DELETE CASCADE;

ALTER TABLE crime_batch_crime_version
    ADD CONSTRAINT fk_cribatcriver_on_crime_version FOREIGN KEY (crime_version_id) REFERENCES crime_version (id)
        ON DELETE CASCADE;