CREATE TABLE crime_matching_result
(
    id                    UUID NOT NULL,
    crime_version_id      UUID NOT NULL,
    crime_matching_run_id UUID NOT NULL,
    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_crime_matching_result PRIMARY KEY (id)
);

CREATE TABLE crime_matching_result_device_wearer
(
    id                       UUID   NOT NULL,
    crime_matching_result_id UUID   NOT NULL,
    device_id                BIGINT NOT NULL,
    name                     VARCHAR(255) NOT NULL,
    nomis_id                 VARCHAR(255) NOT NULL,
    CONSTRAINT pk_crime_matching_result_device_wearer PRIMARY KEY (id)
);

CREATE TABLE crime_matching_result_position
(
    id                                     UUID             NOT NULL,
    crime_matching_result_device_wearer_id UUID             NOT NULL,
    latitude                               DOUBLE PRECISION NOT NULL,
    longitude                              DOUBLE PRECISION NOT NULL,
    captured_date_time                     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    sequence_label                         VARCHAR(255) NOT NULL,
    confidence_circle                      INTEGER          NOT NULL,
    CONSTRAINT pk_crime_matching_result_position PRIMARY KEY (id)
);

CREATE TABLE crime_matching_run
(
    id                UUID NOT NULL,
    crime_batch_id    UUID NOT NULL,
    algorithm_version VARCHAR(255) NOT NULL,
    trigger_type      VARCHAR(255) NOT NULL,
    status            VARCHAR(255) NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    matching_started  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    matching_ended    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_crime_matching_run PRIMARY KEY (id)
);

ALTER TABLE crime_matching_result
    ADD CONSTRAINT uc_crime_matching_run_id_crime_version_id UNIQUE (crime_matching_run_id, crime_version_id);

ALTER TABLE crime_matching_result_device_wearer
    ADD CONSTRAINT uc_crime_matching_result_id_device_id UNIQUE (crime_matching_result_id, device_id);

ALTER TABLE crime_matching_result_position
    ADD CONSTRAINT FK_CRIMEMATCHINGRESULTPOSITION_ON_CRIMEMATCHINGRESULTDEVICEWEAR FOREIGN KEY (crime_matching_result_device_wearer_id) REFERENCES crime_matching_result_device_wearer (id)
        ON DELETE CASCADE;

ALTER TABLE crime_matching_result_device_wearer
    ADD CONSTRAINT FK_CRIME_MATCHING_RESULT_DEVICE_WEARER_ON_CRIME_MATCHING_RESULT FOREIGN KEY (crime_matching_result_id) REFERENCES crime_matching_result (id)
        ON DELETE CASCADE;

ALTER TABLE crime_matching_result
    ADD CONSTRAINT FK_CRIME_MATCHING_RESULT_ON_CRIME_MATCHING_RUN FOREIGN KEY (crime_matching_run_id) REFERENCES crime_matching_run (id)
        ON DELETE CASCADE;

ALTER TABLE crime_matching_result
    ADD CONSTRAINT FK_CRIME_MATCHING_RESULT_ON_CRIME_VERSION FOREIGN KEY (crime_version_id) REFERENCES crime_version (id)
        ON DELETE CASCADE;

ALTER TABLE crime_matching_run
    ADD CONSTRAINT FK_CRIME_MATCHING_RUN_ON_CRIME_BATCH FOREIGN KEY (crime_batch_id) REFERENCES crime_batch (id)
        ON DELETE CASCADE;