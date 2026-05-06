CREATE TABLE crime_version_update
(
    id               UUID NOT NULL,
    crime_version_id UUID NOT NULL,
    field_name       VARCHAR(30) NOT NULL,
    CONSTRAINT pk_crime_version_update PRIMARY KEY (id)
);

ALTER TABLE crime_version_update
    ADD CONSTRAINT FK_CRIME_VERSION_UPDATE_ON_CRIME_VERSION FOREIGN KEY (crime_version_id) REFERENCES crime_version (id)
        ON DELETE CASCADE;